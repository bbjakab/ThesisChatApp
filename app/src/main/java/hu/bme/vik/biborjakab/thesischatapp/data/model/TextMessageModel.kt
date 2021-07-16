package hu.bme.vik.biborjakab.thesischatapp.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp


/**
 * Szöveges üzenet modellje
 * @property senderUID a feladó user id-ja
 * @property message az üzenet
 * @property timestamp az üzenet küldésének időpontja - a szerver tölti ki
 */
class TextMessageModel() {
    var senderUID: String? = null
    var message: String? = null
    @ServerTimestamp
    var timestamp: Timestamp? = null

    constructor(senderUID: String, message: String, timestamp: Timestamp?) : this() {
        this.senderUID = senderUID
        this.message = message
        this.timestamp = timestamp
    }

}