package hu.bme.vik.biborjakab.thesischatapp.data

import android.util.Log
import com.google.android.gms.tasks.Tasks
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.Timestamp
import com.google.firebase.firestore.*
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import hu.bme.vik.biborjakab.thesischatapp.data.model.MessagePartnerModel
import hu.bme.vik.biborjakab.thesischatapp.data.model.TextMessageModel
import hu.bme.vik.biborjakab.thesischatapp.data.model.UserModel
import hu.bme.vik.biborjakab.thesischatapp.util.*
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import org.greenrobot.eventbus.EventBus
import java.util.*

object Database {

    private val TAG = javaClass.name

    private val db = Firebase.firestore

    /**
     * This is the registration of new message if
     * the user is in a chat window, otherwise null
     */
    private var singleChatRoomListener: ListenerRegistration? = null

    private val chatRoomReferenceMap: MutableMap<String, DocumentReference> = mutableMapOf()

    suspend fun attachNewMessagesListener(messagePartnerUID: String) {
        if (!isUserSignedIn()) {
            return
        }

        val uids = mutableListOf(Auth.currentUser!!.uid, messagePartnerUID).sorted()

        val chatRoomQueryResult = db.collection("chatRooms")
                .whereEqualTo("memberAUID", uids[0])
                .whereEqualTo("memberBUID", uids[1])
                .get().await()

        if (chatRoomQueryResult.size() != 1) {
            Log.d(TAG, "single chatroom listener error: not single")
            return

        } else {
            singleChatRoomListener =
            chatRoomQueryResult.documents[0].reference.collection("messages")
                .whereGreaterThan("timestamp", Timestamp.now())
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(1)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            Log.d(TAG, "single chatroom listener failed")
                            return@addSnapshotListener
                        }
                        for (dc in snapshot!!.documentChanges) {
                            if  (dc.type == DocumentChange.Type.ADDED) {
                                val message = dc.document.toObject<TextMessageModel>()
                                EventBus.getDefault().post(NewMessageInChatRoomEvent(message))
                            }
                            else {
                                Log.d(TAG, "SINGLE CHATROOM LISTENER message not added, ${dc.type} instead")
                            }
                        }
            }
        }
    }

    fun detachNewMessagesListener() {
        singleChatRoomListener?.remove()
    }

    suspend fun getMessagesBatch(messagePartnerUID: String, lastQueriedMessage: TextMessageModel? = null) {
        if (!isUserSignedIn()) {
            return
        }

        val uids = listOf(Auth.currentUser!!.uid, messagePartnerUID).sorted()

        try {
            if (!chatRoomReferenceMap.containsKey(messagePartnerUID)) {

                val chatRoomQueryResult =
                        db.collection("chatRooms")
                        .whereEqualTo("memberAUID", uids[0])
                        .whereEqualTo("memberBUID", uids[1])
                        .get().await()


                when(chatRoomQueryResult.size()) {
                    0 -> {
                        //chatroom not exists yet, it will be created on first message sent
                        throw NoResultException()
                    }
                    1 -> {
                        chatRoomReferenceMap.put(messagePartnerUID, chatRoomQueryResult.documents[0].reference)
                    }
                    else -> {
                        throw TooManyResultsException()
                    }
                }
            }
            val first: Boolean
            val messagePagingQuery = if (lastQueriedMessage == null) {
                                    first = true
                                    chatRoomReferenceMap[messagePartnerUID]!!.collection("messages")
                                            .orderBy("timestamp", Query.Direction.DESCENDING)
                                            .limit(PAGINATING_AMOUNT_CHAT_MESSAGES)
                                    } else {
                                        first = false
                                        chatRoomReferenceMap[messagePartnerUID]!!.collection("messages")
                                                .orderBy("timestamp", Query.Direction.DESCENDING)
                                                .startAfter(lastQueriedMessage.timestamp)
                                                .limit(PAGINATING_AMOUNT_CHAT_MESSAGES)
                                    }

            val getMessagesResult = messagePagingQuery.get().await()

            val messages: MutableList<TextMessageModel> = mutableListOf()
            if (!getMessagesResult.isEmpty) {
                for (document in getMessagesResult.documents) {
                    messages.add(TextMessageModel(
                            document.get("senderUID") as String,
                            document.get("message") as String,
                            document.get("timestamp") as Timestamp?
                    ))
                }

            }

            EventBus.getDefault().post(PastMessagesEvent(messages, first, null))

        }
        catch (e: Exception) {
            when (e) {
                is NoResultException -> {
                    EventBus.getDefault().post(NoChatRoomExceptionEvent())
                }
                is TooManyResultsException -> {
                    Log.d(TAG, "GET MESSAGES error too many chatrooms")
                }
                else -> {
                    Log.d(TAG, "GET MESSAGES error $e")
                }
            }
        }
    }

    suspend fun sendTextMessage(messagePartnerUID: String, messageText: String) {
        if (!isUserSignedIn()) {
            return
        }

        try {
            //check connectivity
            Auth.currentUser!!.reload().await()

            if (!chatRoomReferenceMap.containsKey(messagePartnerUID)) {

                val uids = listOf(Auth.currentUser!!.uid, messagePartnerUID).sorted()

                val dataMemberUIDs = mapOf(
                        "memberAUID" to uids[0],
                        "memberBUID" to uids[1],
                        "memberUIDs" to uids
                )

                val chatRoomDocRef = db.collection("chatRooms").add(dataMemberUIDs).await()
                chatRoomReferenceMap.put(messagePartnerUID, chatRoomDocRef)
            }
            //chatroomreference is filled at this point
            val message = TextMessageModel(
                    senderUID = Auth.currentUser!!.uid,
                    message = messageText,
                    timestamp = null
            )

            chatRoomReferenceMap[messagePartnerUID]!!.collection("messages").add(message).await()

        } catch (e: Exception) {
            Log.d(TAG, "SEND MESSAGE error $e")
            if (e is FirebaseNetworkException) {
                EventBus.getDefault().post(NetworkErrorEvent())
            }

        }
    }

    suspend fun getMessagePartnersNow() {
        if (!isUserSignedIn()) {
            return
        }

        try {
            val chatRoomsResult = db.collection("chatRooms")
                    .whereArrayContains("memberUIDs", Auth.currentUser!!.uid).get().await()
            val messagePartners: MutableList<MessagePartnerModel> = mutableListOf()

            chatRoomsResult.documents.forEach {
                val memberUIDs: List<*> = it.get("memberUIDs") as List<*>
                val messagePartnerUID =
                        if (memberUIDs[0] == Auth.currentUser!!.uid) {
                            memberUIDs[1].toString()
                        } else {
                            memberUIDs[0].toString()
                        }

                val partnerUserModelDeferred =
                        GlobalScope.async(Dispatchers.IO) {
                            val userDocument = Tasks.await(db.collection("users")
                                    .document(messagePartnerUID).get())
                            return@async UserModel(messagePartnerUID,
                                    userDocument["displayName"] as String,
                                    userDocument["email"] as String)
                        }

                val getLatestMessage = Tasks.await(it.reference.collection("messages")
                        .orderBy("timestamp", Query.Direction.DESCENDING)
                        .limit(1)
                        .get())

                if (getLatestMessage.size() != 1) {
                    //ha nincs 1 üzenetük se, akkor nem kell listázni a partnerek között
                    return@forEach
                }

                val lastMessage = getLatestMessage.documents[0].toObject<TextMessageModel>()!!
                val messagePartner = MessagePartnerModel(partnerUserModelDeferred.await(), lastMessage)
                messagePartners.add(messagePartner)
            }

            messagePartners.sortByDescending { it.lastMessage.timestamp }
            EventBus.getDefault().postSticky(GetMessagePartnersEvent(messagePartners))

        } catch (e: Exception) {
            Log.d(TAG, "GETMESSAGEPARTNERSNOW error: $e")
        }
    }

    suspend fun searchUsers(orderBy: OrderBy, lastQueriedUserDoc: DocumentSnapshot? = null) {
        if (!isUserSignedIn()) {
            return
        }

        val baseQuery = db.collection("users")

        var actualQuery =
                when (orderBy) {
                    OrderBy.DISPLAY_NAME_ASC -> {
                        baseQuery.orderBy("displayName", Query.Direction.ASCENDING).orderBy("email", Query.Direction.ASCENDING)
                    }
                    OrderBy.DISPLAY_NAME_DESC -> {
                        baseQuery.orderBy("displayName", Query.Direction.DESCENDING).orderBy("email", Query.Direction.DESCENDING)
                    }
                    OrderBy.EMAIL_ADDRESS_ASC -> {
                        baseQuery.orderBy("email", Query.Direction.ASCENDING)
                    }
                    OrderBy.EMAIL_ADDRESS_DESC -> {
                        baseQuery.orderBy("email", Query.Direction.DESCENDING)
                    }
                }

        if (lastQueriedUserDoc != null) {
            actualQuery = actualQuery.startAfter(lastQueriedUserDoc)
        }

        actualQuery = actualQuery.limit(PAGINATING_AMOUNT_USER_RESULTS)

        try {
            val documentsSnapshot = actualQuery.get().await()
            val usersList = mutableListOf<UserModel>()
            for (doc in documentsSnapshot) {
                usersList.add(UserModel(doc.id,
                        doc.get("displayName") as String,
                        doc.get("email") as String))
            }
            EventBus.getDefault().post(SearchUsersResultEvent(usersList as List<UserModel>, documentsSnapshot.last(), null))
        } catch (e: Exception) {
            Log.d(TAG, "getallusers error $e")
            EventBus.getDefault().post(SearchUsersResultEvent(null, null, e))
        }
    }

    private fun isUserSignedIn(): Boolean {
        return Auth.currentUser != null
    }

    fun cleanUp() {
        detachNewMessagesListener()
        chatRoomReferenceMap.clear()
    }
}

class AnyNewMessagesListener {
    private val TAG = "ANYNEWMSG"
    private var userInChatRoomsListener: ListenerRegistration? = null
    private val lastMessageFromAnyChatRoomListeners = mutableListOf<ListenerRegistration>()

    fun attach() {
        if (Auth.currentUser == null) {
            return
        }

        userInChatRoomsListener =
            Firebase.firestore.collection("chatRooms")
                .whereArrayContains("memberUIDs", Auth.currentUser!!.uid)
                .addSnapshotListener { chatRoomsQueryResult, error ->

                    GlobalScope.launch(Dispatchers.IO) chatRoomSnapshotListener@{
                        if (error != null) {
                            Log.d(TAG, "CHATROOMS listen failed: $error")
                            return@chatRoomSnapshotListener
                        }

                        if (chatRoomsQueryResult != null) {
                            for (dc in chatRoomsQueryResult.documentChanges) {
                                when (dc.type) {
                                    DocumentChange.Type.ADDED -> {
                                        val memberUIDs: List<*> =
                                            dc.document.get("memberUIDs") as List<*>
                                        val messagePartnerUID =
                                            if (memberUIDs[0] == Auth.currentUser!!.uid) {
                                                memberUIDs[1].toString()
                                            } else {
                                                memberUIDs[0].toString()
                                            }

                                        val partnerUserModelDeferred =
                                            GlobalScope.async(Dispatchers.IO) {
                                                val userDocument = Firebase.firestore.collection("users")
                                                    .document(messagePartnerUID).get().await()
                                                return@async UserModel(messagePartnerUID,
                                                    userDocument["displayName"] as String,
                                                    userDocument["email"] as String)
                                            }

                                        val registration =
                                            dc.document.reference.collection("messages")
                                                .whereGreaterThanOrEqualTo("timestamp", Timestamp.now())
                                                .orderBy("timestamp", Query.Direction.DESCENDING)
                                                .limit(1)
                                                .addSnapshotListener { latestMessageSnapshot, err ->

                                                    GlobalScope.launch(Dispatchers.IO) latestMessageSnapshotListener@{
                                                        if (err != null) {
                                                            Log.d(TAG,"ANY NEW MESSAGES listen failed: $err")
                                                            return@latestMessageSnapshotListener
                                                        }

                                                        val messagePartner = MessagePartnerModel(partnerUserModelDeferred.await())

                                                        if (latestMessageSnapshot!!.size() != 1) {
                                                            if (latestMessageSnapshot.size() == 0) {
                                                                //how many times this runs == how many chatrooms the user has
                                                            }
                                                            return@latestMessageSnapshotListener
                                                        }

                                                        val latestMessage = latestMessageSnapshot.documents[0].toObject<TextMessageModel>()!!

                                                        messagePartner.lastMessage = latestMessage

                                                        if (latestMessage.senderUID != Auth.currentUser!!.uid) {
                                                            EventBus.getDefault().post(AnyNewMessageEvent(messagePartner))
                                                        }
                                                        EventBus.getDefault().postSticky(MessagePartnerUpdateEvent(messagePartner))
                                                    }
                                                }
                                        lastMessageFromAnyChatRoomListeners.add(registration)
                                    }
                                    else -> {
                                        Log.d(TAG, "oops ANY NEW CHATROOMS: document change != added")
                                    }
                                }
                            }
                        }
                    }
                }
    }

    fun detach() {
        userInChatRoomsListener?.remove()
        lastMessageFromAnyChatRoomListeners.forEach {
            it.remove()
        }
        lastMessageFromAnyChatRoomListeners.clear()
    }
}
