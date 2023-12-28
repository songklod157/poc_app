package com.example.poc_app

import android.Manifest
import android.R
import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.BlockedNumberContract.BlockedNumbers
import android.provider.ContactsContract
import android.provider.Telephony
import android.telecom.TelecomManager
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import io.flutter.embedding.android.FlutterActivity
import io.flutter.plugin.common.MethodChannel
import java.util.*


class MainActivity: FlutterActivity() {

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MethodChannel(flutterEngine!!.dartExecutor.binaryMessenger, CHANNEL)
                .setMethodCallHandler { call, result ->
                    when (call.method) {
                        "blockContact" -> {
                            val contactId = call.argument<String>("contactId")
//                            blockContact(applicationContext, contactId!!)
                            result.success(null)
                        }

                        "unblockContact" -> {
                            val contactId = call.argument<String>("contactId")
                            unblockContact(applicationContext, contactId!!)
                            result.success(null)
                        }

                        "getBlockedContacts" -> {
                            val phoneNumber = call.argument<String>("phoneNumber")
                            getBlockedContacts(applicationContext, phoneNumber!!)
                            result.success(null)
                        }

                        "blockPhoneNumber" -> {
                            val blockNumber = call.argument<String>("phoneNumber")
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                blockPhoneNumber(blockNumber!!)
                            }
                            result.success(null)
                        }
                        "getBlockedNumbers" -> {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                result.success(getBlockedNumbers())
                            };
                        }

                        else -> {
                            result.notImplemented()
                        }
                    }
                }
    }

    private fun getContactById(context: Context, contactId: String): Contact? {
        val contentResolver: ContentResolver = context.contentResolver
        val uri: Uri = ContactsContract.Contacts.CONTENT_URI

        val projection = arrayOf(
                ContactsContract.Contacts._ID,
                ContactsContract.Contacts.DISPLAY_NAME,
                ContactsContract.Contacts.HAS_PHONE_NUMBER
        )

        val selection = ContactsContract.Contacts._ID + " = ?"
        val selectionArgs = arrayOf(contactId)

        val cursor: Cursor? = contentResolver.query(uri, projection, selection, selectionArgs, null)

        if (cursor != null && cursor.moveToFirst()) {
            val contact: Contact = Contact.fromCursor(cursor)
            cursor.close()
            return contact
        }

        return null
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun blockPhoneNumber(phoneNumber: String) {
        checkDefaultDialer()
        requestSetDefaultSms()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)
                == PackageManager.PERMISSION_GRANTED) {
            // Permission granted
        }
        val values = ContentValues()
        values.put(BlockedNumbers.COLUMN_ORIGINAL_NUMBER, "0612786089")
        values.put(BlockedNumbers.COLUMN_E164_NUMBER, "+66612786089")
        val uri = contentResolver.insert(BlockedNumbers.CONTENT_URI, values)
    }
//    private fun requestSetDefaultDialer() {
//        val telecomManager = getSystemService(Context.TELECOM_SERVICE) as TelecomManager
//    }
    private fun requestSetDefaultSms() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            val intent = Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT)
                    .putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, context.packageName)
            context.startActivity(intent)
        }
    }

    private fun updateContactBlockedStatus(contactId: String, isBlocked: Boolean) {
        val contentResolver: ContentResolver = applicationContext.contentResolver
        val updateUri = ContactsContract.Data.CONTENT_URI
        val values = ContentValues()

        // Assuming you have a custom field 'isBlocked' in your contacts
        values.put(ContactsContract.Data.DATA1, isBlocked.toString())

        val selection = "${ContactsContract.Data.CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?"
        val selectionArgs = arrayOf(contactId, "vnd.android.cursor.item/com.example.isBlocked")

        contentResolver.update(updateUri, values, selection, selectionArgs)
    }
//    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
//    private fun blockContact(context: Context, contactId: String) {
//        val telecomManager = getSystemService(Context.TELECOM_SERVICE) as TelecomManager
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
//            context.startActivity(telecomManager.createManageBlockedNumbersIntent(), null)
//        };
//        // Retrieve the set of blocked contact IDs from SharedPreferences
//        val prefs = getPreferences(MODE_PRIVATE)
//        val blockedContactIds = prefs.getStringSet("blockedContactIds", HashSet())
//
//        // Add the contact ID to the set of blocked contact IDs
//        blockedContactIds!!.add(contactId)
//
//        // Save the updated set back to SharedPreferences
//        val editor = prefs.edit()
//        editor.putStringSet("blockedContactIds", blockedContactIds)
//        editor.apply()
//    }

    private fun unblockContact(context: Context, contactId: String) {
        // Retrieve the set of blocked contact IDs from SharedPreferences
        val prefs = getPreferences(MODE_PRIVATE)
        val blockedContactIds = prefs.getStringSet("blockedContactIds", HashSet())

        // Remove the contact ID from the set of blocked contact IDs
        blockedContactIds!!.remove(contactId)

        // Save the updated set back to SharedPreferences
        val editor = prefs.edit()
        editor.putStringSet("blockedContactIds", blockedContactIds)
        editor.apply()
    }

    @SuppressLint("NewApi")
    private fun getBlockedContacts(context: Context, phoneNumber: String) {
        val telecomManager = context.getSystemService(TELECOM_SERVICE) as TelecomManager
        val values = ContentValues()
        values.put(BlockedNumbers.COLUMN_ORIGINAL_NUMBER, "1112")
        val uri = contentResolver.insert(BlockedNumbers.CONTENT_URI, values)

    }


    private fun isContactBlocked(context: Context, contactId: String): Boolean {
        // Retrieve the set of blocked contact IDs from SharedPreferences
        val prefs = getPreferences(MODE_PRIVATE)
        val blockedContactIds = prefs.getStringSet("blockedContactIds", HashSet())

        // Check if the provided contact ID is in the set of blocked contact IDs
        return blockedContactIds!!.contains(contactId)
    }
    private fun getBlockedNumbers(): List<String>? {
        checkDefaultDialer()
//        requestSetDefaultSms()
        val blockedNumbersList: MutableList<String> = ArrayList()

        // Check if the app has permission to read phone state
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                checkSelfPermission(Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
            // Content URI for Blocked Numbers
            val blockedNumbersUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                BlockedNumbers.CONTENT_URI
            } else {
                TODO("VERSION.SDK_INT < N")
            }

            // Projection for the query (columns to retrieve)
            val projection = arrayOf(
                    BlockedNumbers.COLUMN_ID,
                    BlockedNumbers.COLUMN_ORIGINAL_NUMBER
            )

            // Perform the query
            val cursor = contentResolver.query(blockedNumbersUri, projection, null, null, null)

            // Check if there are blocked numbers
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    val columnIndex = cursor.getColumnIndex(BlockedNumbers.COLUMN_ORIGINAL_NUMBER)

                    // Check if the column exists in the cursor

                    // Check if the column exists in the cursor
                    if (columnIndex != -1) {
                        val originalNumber = cursor.getString(columnIndex)

                        // Add the blocked number to the list
                        blockedNumbersList.add(originalNumber)
                    } else {
                        // Handle the case where the column does not exist
                        Log.e("BlockedNumbers", "COLUMN_ORIGINAL_NUMBER not found in the cursor")
                    }
                } while (cursor.moveToNext())

                // Close the cursor when done
                cursor.close()
            }
        }
        return blockedNumbersList
    }
    private val REQUEST_CODE_SET_DEFAULT_DIALER=200
    private fun checkDefaultDialer() {
        val telecomManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getSystemService(TELECOM_SERVICE) as TelecomManager
        } else {
            TODO("VERSION.SDK_INT < LOLLIPOP")
        }

        if (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    telecomManager.defaultDialerPackage != packageName
                } else {
                    TODO("VERSION.SDK_INT < M")
                }) {
            // Your app is not the default dialer; proceed to request
            val intent = Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER)
                    .putExtra(TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, packageName)
            startActivity(intent)
        }
//        val telecomManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//            getSystemService(TELECOM_SERVICE) as TelecomManager
//        } else {
//            TODO("VERSION.SDK_INT < LOLLIPOP")
//        }
        val isAlreadyDefaultDialer = packageName == telecomManager.defaultDialerPackage
        if (isAlreadyDefaultDialer)
            return
        val intent = Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER)
                .putExtra(TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, packageName)
        startActivityForResult(intent, REQUEST_CODE_SET_DEFAULT_DIALER)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            REQUEST_CODE_SET_DEFAULT_DIALER -> checkSetDefaultDialerResult(resultCode)
        }
    }

    private fun checkSetDefaultDialerResult(resultCode: Int) {
        val message = when (resultCode) {
            RESULT_OK       -> "User accepted request to become default dialer"
            RESULT_CANCELED -> "User declined request to become default dialer"
            else            -> "Unexpected result code $resultCode"
        }

        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

    }
    companion object {
        private const val CHANNEL = "songklod.com/battery"
    }
}

// In Contact.kt

data class Contact(
        val id: String,
        val displayName: String?,
        val hasPhoneNumber: Boolean
) {
    companion object {
        fun fromCursor(cursor: Cursor): Contact {
            val idIndex = cursor.getColumnIndex(ContactsContract.Contacts._ID)
            val nameIndex = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
            val hasPhoneNumberIndex = cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)

            val contactId = cursor.getString(idIndex)
            val contactName = cursor.getString(nameIndex)
            val hasPhoneNumber = cursor.getInt(hasPhoneNumberIndex) > 0

            return Contact(contactId, contactName, hasPhoneNumber)
        }
    }
}

