package hu.bme.vik.biborjakab.thesischatapp.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import hu.bme.vik.biborjakab.thesischatapp.util.UserLogInEvent
import hu.bme.vik.biborjakab.thesischatapp.util.UserLogOutEvent
import hu.bme.vik.biborjakab.thesischatapp.util.UserLoginSuccessEvent
import hu.bme.vik.biborjakab.thesischatapp.util.UserRegisterEvent
import kotlinx.coroutines.tasks.await
import org.greenrobot.eventbus.EventBus

/**
 * Repository objektum felhasználók kezeléséhez a Firebase rendszerben
 */
object Auth {

    private val auth: FirebaseAuth = Firebase.auth

    val currentUser: FirebaseUser?
        get() = auth.currentUser

    /**
     * Bejelenkezteti a felhasználót a Firebase rendszerébe
     * email és jelszó segítségével
     * @param email Bejelentkezéshez használt email cím
     * @param password Bejelentkezéshez használt jelszó
     */
    suspend fun signInWithEmailAndPassword(email: String, password: String){
        try {
            auth.signInWithEmailAndPassword(email, password).await()
            EventBus.getDefault().post(UserLogInEvent(null))
            EventBus.getDefault().post(UserLoginSuccessEvent())
        } catch (e: Exception) {
            EventBus.getDefault().post(UserLogInEvent(e))
        }
    }

    /**
     * Regisztrálja a felhasználót a Firebase rendszerbe
     * @param email email cím
     * @param password jelszó
     * @param myDisplayName felhasználónév
     */
    suspend fun registerWithEmailAndPassword(email: String, password: String, myDisplayName: String) {
        try {
            //create user in firebase
            val regRes = auth.createUserWithEmailAndPassword(email, password).await()

            //update displayname in firebase authentication system
            currentUser!!.updateProfile(UserProfileChangeRequest.Builder().setDisplayName(myDisplayName).build()).await()

            currentUser!!.getIdToken(true).await()

            //add to users collection
            val userData = hashMapOf(
                    "email" to email,
                    "displayName" to myDisplayName
            )
            Firebase.firestore.collection("users")
                .document(regRes.user!!.uid)
                .set(userData).await()



            EventBus.getDefault().post(UserRegisterEvent(null))
            EventBus.getDefault().post(UserLoginSuccessEvent())
        } catch (e: Exception) {
                EventBus.getDefault().post(UserRegisterEvent(e))
        }

    }

    /**
     * Felhasználó kijelentkeztetése
     */
    fun signOut() {
        auth.signOut()
        EventBus.getDefault().post(UserLogOutEvent())
    }

}