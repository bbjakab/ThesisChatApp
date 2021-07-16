package hu.bme.vik.biborjakab.thesischatapp.data.model

/**
 * Az üzenetpartner, amely a felhasználó profilját és legutóbbi üzenetét tartalmazza
 * @see UserModel
 * @see TextMessageModel
 */
class MessagePartnerModel(
    val userInfo: UserModel
    ) {

    lateinit var lastMessage: TextMessageModel

    constructor(user: UserModel, lastMessage: TextMessageModel): this(user) {
        this.lastMessage = lastMessage
    }
}

