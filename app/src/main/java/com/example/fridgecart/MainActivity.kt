package com.example.fridgecart

import android.os.Bundle
import com.google.android.material.snackbar.Snackbar
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.ContentValues
import android.content.DialogInterface
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.content.ContentValues.TAG
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import android.view.Menu
import android.view.MenuItem
import com.example.fridgecart.databinding.ActivityMainBinding
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private var deviceName: String? = null
    private var deviceAddress: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val buttonConnect = findViewById<Button>(R.id.button_bluetooth)
        val blueText = findViewById<TextView>(R.id.textViewBlue)

        //If a Bluetooth device has been selected from SelectDeviceActivity
        deviceName = intent.getStringExtra("deviceName")
        if (deviceName != null) {
            //Get the device address to make BT connection
            deviceAddress = intent.getStringExtra("deviceAddress")

            //buttonConnect.isEnabled = false
            buttonConnect.visibility = View.INVISIBLE
            blueText.text = "Bluetooth Connected"

            /*
            Once deviceName is found, the code will call a new thread to create
            a bluetooth connection to the selected device
            */
            val bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager

            createConnThread = MainActivity.CreateConnectThread(bluetoothManager.getAdapter(), deviceAddress)
            createConnThread!!.start()
        }

        handle = object : Handler(Looper.getMainLooper()) {
            override fun handleMessage(msg: Message) {
                when (msg.what) {
                    CONNECTING_STATUS -> when (msg.arg1) {
                        1 -> {
                            blueText.text = "Connected to $deviceName"
                            buttonConnect.isEnabled = true
                        }
                        -1 -> {
                            blueText.text = "Device failed to connect"
                            buttonConnect.isEnabled = true
                        }
                    }
                    MESSAGE_READ -> {
                        val arduinoMsg = msg.obj.toString() // Read message from Arduino
                        when (arduinoMsg.lowercase()) {
                            "led is turned on" -> {
                                //imageView.setBackgroundColor(resources.getColor(R.color.colorOn))
                                //textViewInfo.text = "Arduino Message : $arduinoMsg"
                            }
                            "led is turned off" -> {
                                //imageView.setBackgroundColor(resources.getColor(R.color.colorOff))
                                //textViewInfo.text = "Arduino Message : $arduinoMsg"
                            }
                        }
                    }
                }
            }
        }

        /*buttonConnect.setOnClickListener {
            val intent = Intent(this@MainActivity, SelectDeviceActivity::class.java)
            startActivity(intent)
        }*/
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        val navController = findNavController(R.id.nav_host_fragment_content_main)
        appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfiguration)

//        binding.fab.setOnClickListener { view ->
//            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                    .setAction("Action", null).show()
//        }
    }

    class CreateConnectThread(bluetoothAdapter: BluetoothAdapter, address: String?) : Thread() {
        override fun run() {
            //Cancel discovery, otherwise it slows connection
            val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
            bluetoothAdapter.cancelDiscovery()
            try {
                //connect to the remote device through the socket. This call blocks till it succeeds or excepts
                mmSocket!!.connect()
                Log.e("Status", "Device connected")
                handle!!.obtainMessage(CONNECTING_STATUS, 1, -2).sendToTarget()
            } catch (connectException: IOException) {
                //Unable to connect, close the socket and return
                try {
                    mmSocket!!.close()
                    Log.e("Status", "Cannot connect to device")
                    handle!!.obtainMessage(CONNECTING_STATUS, -1, -1).sendToTarget()
                } catch (closeException: IOException) {
                    Log.e(ContentValues.TAG, "Could not close the client socket", closeException)
                }
                return
            }

            //Connection Succeeded. Perform connection work in a seperate thread
            connThread = MainActivity.ConnectedThread(mmSocket)
            connThread!!.run()
        }

        //Closes client socket and causes the thread to finish.
        fun cancel() {
            try {
                mmSocket!!.close()

            } catch (e: IOException) {
                Log.e(ContentValues.TAG, "Could not close the client socket", e)
            }
        }

        init {
            //assign and use a temp object that is later assigned to mmSocket

            val bluetoothDevice = bluetoothAdapter.getRemoteDevice(address)
            var tmp: BluetoothSocket? = null
            val uuid = bluetoothDevice.uuids[0].uuid
            try {
                /*
                Get a BluetoothSocket to connect with the given BluetoothDevice.
                Due to Android device varieties,the method below may not work fo different devices.
                You should try using other methods i.e. :
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
                 */
                tmp = bluetoothDevice.createInsecureRfcommSocketToServiceRecord(uuid)
            } catch (e: IOException) {
                Log.e(ContentValues.TAG, "Socket's create() method failed", e)
            }
            MainActivity.mmSocket = tmp
        }
    }

    class ConnectedThread(private val mmSocket: BluetoothSocket?) : Thread() {
        private val mmInStream: InputStream?
        private val mmOutStream: OutputStream?
        override fun run() {
            val buffer = ByteArray(1024)
            var bytes = 0
            var tempString: String?
            while (true) {
                try {
                    /*
                    Read from the InputStream from Arduino until Termination character is reached.
                    Then send the whole string message to GUI handler.
                    */
                    buffer[bytes] = mmInStream!!.read().toByte()
                    var readMessage: String
                    tempString = String(buffer, 0, bytes)
                    if (tempString == "/n") {
                        readMessage = String(buffer, 0, bytes)
                        Log.e( "Arduino Message", readMessage)
                        handle!!.obtainMessage(MESSAGE_READ, readMessage).sendToTarget()
                        bytes = 0
                    } else {
                        bytes++
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                    break
                }
            }
        }

        /* Call this from the main activity to send data to the remote device */
        fun write(input: String?) {
            val bytes = input!!.toByteArray()
            try {
                mmOutStream!!.write(bytes)
            } catch (e: IOException) {
                Log.e( "Send Error", "Unable to send message", e)
            }
        }

        /* Call this from the main activity to shutdown the connection*/
        fun cancel() {
            try {
                mmSocket!!.close()
            } catch (e: IOException) {}
        }

        init {
            var tempIn: InputStream? = null
            var tempOut: OutputStream? = null

            /* Get the input and output streams, using temp objects as member streams are final */
            try {
                tempIn = mmSocket!!.inputStream
                tempOut = mmSocket.outputStream
            } catch (e: IOException) {}
            mmInStream = tempIn
            mmOutStream = tempOut
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }

    companion object {
        var handle: Handler? = null
        var mmSocket: BluetoothSocket? = null
        var connThread: MainActivity.ConnectedThread? = null
        var createConnThread: MainActivity.CreateConnectThread? = null
        private const val CONNECTING_STATUS =
            1 // used in bluetooth handler to identify message status
        private const val MESSAGE_READ = 2 // used in bluetooth handler to identify message update
    }
}