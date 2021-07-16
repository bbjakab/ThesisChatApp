package hu.bme.vik.biborjakab.thesischatapp.data.model

import android.os.Parcel
import android.os.Parcelable

/**
 * Felhasználót reprezentáló modell
 * @property userID firebase által kiosztott egyedi azonosító
 * @property displayName felhasználónév
 * @property emailAddress email cím
 */
class UserModel(val userID: String): Parcelable {
    var displayName: String? = null
    var emailAddress: String? = null

    constructor(userID: String, displayName: String, emailAddress: String) : this(userID) {
        this.displayName = displayName
        this.emailAddress = emailAddress
    }

//v-v-v-v-v-v-v--parcelable implementation-v-v-v-v-v-v-v-v

    //csak "teljes" userekkel (minden mező kitöltve)
    constructor(parcel: Parcel): this(parcel.readString()!!, parcel.readString()!!, parcel.readString()!!) {}

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(userID)
        parcel.writeString(displayName)
        parcel.writeString(emailAddress)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<UserModel> {
        override fun createFromParcel(parcel: Parcel): UserModel {
            return UserModel(parcel)
        }

        override fun newArray(size: Int): Array<UserModel?> {
            return arrayOfNulls(size)
        }
    }

}