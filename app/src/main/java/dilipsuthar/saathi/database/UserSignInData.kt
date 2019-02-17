package dilipsuthar.saathi.database

import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.firebase.auth.FirebaseUser

class UserSignInData {

    private var name: String? = null
    private var email: String? = null
    private var photoUrl: String? = null
    private var phoneNum: String? = null

    companion object {

        fun setGoogleUserData(user: FirebaseUser?): UserSignInData {
            val userData = UserSignInData()
            if (user != null) {
                userData.name = user.displayName
                userData.email = user.email
                userData.phoneNum = user.phoneNumber
                userData.photoUrl = user.photoUrl.toString()
            }

            return userData

        }

        fun setPhoneAuthUserData(user: FirebaseUser?): UserSignInData {
            val userData = UserSignInData()
            if (user != null)
                userData.phoneNum = user.phoneNumber

            return userData

        }

    }

    //Getter
    val getName: String? get() = this.name
    val getEmail: String? get() = this.email
    val getPhotoUrl: String? get() = this.photoUrl
    val getPhoneNum: String? get() = this.phoneNum

}