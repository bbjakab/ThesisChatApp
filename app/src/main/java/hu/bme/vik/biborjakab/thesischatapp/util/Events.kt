package hu.bme.vik.biborjakab.thesischatapp.util

import com.google.firebase.firestore.DocumentSnapshot
import hu.bme.vik.biborjakab.thesischatapp.data.model.MessagePartnerModel
import hu.bme.vik.biborjakab.thesischatapp.data.model.TextMessageModel
import hu.bme.vik.biborjakab.thesischatapp.data.model.UserModel

//Event(data, exception): one or the other

/**
 * don't need data because it's in Auth.currentuser
 */
class UserLogInEvent(val exception: Exception?)

/**
 * don't need data because it's in Auth.currentuser
 */
class UserRegisterEvent(val exception: Exception?)

class PastMessagesEvent(val messageList: List<TextMessageModel>?, val isFirst: Boolean, val error: Exception?)
class SearchUsersResultEvent(val usersList: List<UserModel>?, val lastQueriedUserDoc: DocumentSnapshot?, val error: Exception?)
class NewMessageInChatRoomEvent(val message: TextMessageModel)
class GetMessagePartnersEvent(val messagePartners: List<MessagePartnerModel>)
class MessagePartnerUpdateEvent(val messagePartner: MessagePartnerModel)
class AnyNewMessageEvent(val messagePartner: MessagePartnerModel)
class NetworkErrorEvent
class UserLoginSuccessEvent
class UserLogOutEvent
class NoChatRoomExceptionEvent
class NetworkAvailableEvent(val isNetworkAvailable: Boolean)