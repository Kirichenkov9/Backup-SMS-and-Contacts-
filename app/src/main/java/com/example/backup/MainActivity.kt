package com.example.backup

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.ContactsContract
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull
import org.jetbrains.anko.find
import org.jetbrains.anko.toast
import java.io.File
import java.io.FileOutputStream
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.collections.HashSet

class MainActivity : AppCompatActivity() {
    private val PERMISSION = 0

    private var title: TextView? = null
    private var path: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        title = find(R.id.title)
        path = find(R.id.path)

        val statusContacts =
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)

        val statusSMS =
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS)

        val statusWrite =
            ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)


        if (statusContacts == PackageManager.PERMISSION_GRANTED &&
            statusSMS == PackageManager.PERMISSION_GRANTED &&
            statusWrite == PackageManager.PERMISSION_GRANTED
        ) {
            val str = getContacts().toString() + getSMS().toString()

            title?.visibility = View.VISIBLE
            path?.text = saveFile(str)
            path?.visibility = View.VISIBLE
        } else
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.READ_CONTACTS,
                    Manifest.permission.READ_SMS,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ),
                PERMISSION
            )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults[0] != PackageManager.PERMISSION_GRANTED)
                    toast("Предоставьте все необходимые разрешения!")
                else {
                    val str = getContacts().toString() + getSMS().toString()
                    title?.visibility = View.VISIBLE
                    path?.text = saveFile(str) ?: "Возникли какие-то проблемы(((("
                    path?.visibility = View.VISIBLE
                }

            }
        }
        return
    }

    private fun getContacts(): HashMap<String?,ContactModel> {
        val list = HashMap<String?, ContactModel>()

        val contentResolver = contentResolver
        val cursor =
            contentResolver.query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null)

        if (cursor.count > 0) {
            while (cursor.moveToNext()) {
                val id =
                    cursor.getStringOrNull(cursor.getColumnIndex(ContactsContract.Contacts._ID))
                if (cursor.getInt(cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)) > 0) {
                    val cursorInfo =
                        contentResolver.query(
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            null,
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?", arrayOf(id),
                            null
                        )

                    while (cursorInfo.moveToNext()) {
                        val info = ContactModel(
                            id,
                            cursor.getStringOrNull(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)),
                            cursorInfo.getStringOrNull(cursorInfo.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER))
                        )
                        if (id !in list)
                            list[id] = info
                    }

                    cursorInfo.close()
                }
            }
            cursor.close()
        }
        return list
    }

    private fun getSMS(): ArrayList<SMSModel> {
        val messages = ArrayList<SMSModel>()
        val uriSMS = Uri.parse("content://sms/inbox")

        val cur = contentResolver.query(uriSMS, null, null, null, null)

        while (cur != null && cur.moveToNext()) {
            val type: String =
                if (cur.getString(cur.getColumnIndexOrThrow("type")).contains("1"))
                    "inbox"
                else
                    "sent"
            val sms = SMSModel(
                cur.getStringOrNull(cur.getColumnIndex("address")),
                cur.getStringOrNull(cur.getColumnIndexOrThrow("body")),
                cur.getLongOrNull(cur.getColumnIndexOrThrow("date"))?.let { Date(it) },
                type
            )
            messages.add(sms)
        }

        cur?.close()
        return messages
    }


    private fun saveFile(str: String): String? {
        var path: String? = null

        val root = Environment.getExternalStorageDirectory().absolutePath
        val file = File( "$root/backup/backup.txt")

        if (file.exists()) file.delete()

        try {
            val out = FileOutputStream(file)
            out.write(str.toByteArray())
            out.flush()
            out.close()

            path = file.absolutePath.toString()

        } catch (e: Exception) {
            e.printStackTrace()
        }

        return path
    }
}

data class ContactModel(
    val id: String?,
    val name: String?,
    val mobileNumber: String?
)

data class SMSModel(
    val address: String?,
    val content: String?,
    val date: Date?,
    val type: String?
)

