package dilipsuthar.saathi.database

import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore

class Firebase {

    private var task: Task<DocumentSnapshot>? = null

    companion object {
        fun fetchData(firebaseFirestore: FirebaseFirestore, auth: FirebaseAuth): Task<DocumentSnapshot>? {
            val firebase = Firebase()

            firebaseFirestore.collection("Users").document(auth.currentUser!!.uid).get()
                .addOnCompleteListener { task ->
                    if (task.result!!.exists())
                        firebase.task = task
                }

            return firebase.task
        }
    }
}