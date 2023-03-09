package com.ankit.p2pdemo

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.NetworkInfo
import android.net.wifi.p2p.*
import android.net.wifi.p2p.WifiP2pManager.ActionListener
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.ankit.p2pdemo.databinding.FragmentGlassesBinding
import com.ankit.p2pdemo.databinding.FragmentPhoneBinding
import java.net.ServerSocket
import java.util.concurrent.atomic.AtomicBoolean

class PhoneFragment : Fragment(), WifiP2pManager.ConnectionInfoListener {
    lateinit var binding: FragmentPhoneBinding
    var channel: WifiP2pManager.Channel? = null
    lateinit var manager: WifiP2pManager
    val TAG = "Glasses frag"
    val handler = Handler(Looper.getMainLooper())
    var bitmap: Bitmap? = null
    var isConnected = AtomicBoolean(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentPhoneBinding.inflate(inflater, container, false)
        binding.textView2.text = "status: initialising"

        manager = requireActivity().getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
        channel = manager.initialize(requireContext(), Looper.getMainLooper(), null)
        channel?.also { channel ->
        }
        return binding.root
    }

    fun discoverPeers() {
        manager.discoverPeers(channel, object : WifiP2pManager.ActionListener {

            override fun onSuccess() {
                binding.textView2.text = "status: peers discovered | listing items in the log"
            }

            override fun onFailure(reasonCode: Int) {
                binding.textView2.text = "status: peers discover error: ${reasonCode}"
                Log.d(TAG, "onFailure: ${reasonCode}")

            }
        })
    }

    fun connectToGlass(device: WifiP2pDevice) {
        val config = WifiP2pConfig()
        config.deviceAddress = device.deviceAddress
        channel?.also { channel ->
            manager.connect(channel, config, object : WifiP2pManager.ActionListener {

                override fun onSuccess() {
                    binding.textView2.text =
                        "status: P2P Ankit device connected | initializing serverSocket"
                    Log.d(TAG, "onSuccess: connected")
                    isConnected.set(true)
                    transferData()
                    manager.stopPeerDiscovery(channel, object : ActionListener{
                        override fun onSuccess() {
                            Log.d(TAG, "onSuccess:stopPeerDiscovery")
                        }

                        override fun onFailure(p0: Int) {
                            Log.d(TAG, "onFailure:stopPeerDiscovery")
                        }
                    })
                }

                override fun onFailure(reason: Int) {
                    Log.d(TAG, "onFailure: ${reason}")
                }
            }
            )
        }
    }

    fun transferData() {
        Thread(Runnable {

            val serverSocket = ServerSocket(8888)
            Log.d(TAG, "transferData: waiting for client on 8888")

            serverSocket.use {
                val client = serverSocket.accept()

                bitmap = client.getInputStream()
                    .use(BitmapFactory::decodeStream)

                handler.post {
                    binding.textView2.text = "status: P2P Ankit device connected | file received"
                    binding.imageView.setImageBitmap(bitmap)

                    //[0,10,0]
                    //[0,001,0]


                    //[0,100,0]
                    //[0,101,0]

                    //image
                    //[0,102,4086,...........,0]

                    //touch
                    //[0,103,20,700,0]
                }

                serverSocket.close()
            }

            Log.d(TAG, "transferData: waiting finished for client on 8888")
        }).start()
    }

    /* register the broadcast receiver with the intent values to be matched */
    override fun onResume() {
        super.onResume()
        val intentFilter = IntentFilter().apply {
            addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
        }
        requireContext().registerReceiver(glassesWiFiDirectBR, intentFilter)
    }

    /* unregister the broadcast receiver */
    override fun onPause() {
        super.onPause()
        requireContext().unregisterReceiver(glassesWiFiDirectBR)
    }


    val glassesWiFiDirectBR = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action: String = intent!!.action!!
            when (action) {
                WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION -> {
                    val state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1)
                    when (state) {
                        WifiP2pManager.WIFI_P2P_STATE_ENABLED -> {
                            binding.textView2.text = "status: P2P enabled | discovering peers"
                            discoverPeers()
                        }
                        else -> {
                            binding.textView2.text = "status: P2P not enabled"
                        }
                    }
                }
                WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
                    Log.d(TAG, "WIFI_P2P_PEERS_CHANGED_ACTION: ")
                    manager.requestPeers(channel) { peers: WifiP2pDeviceList? ->
                        Log.d(TAG, "discovered : ${peers?.deviceList?.size}")
                        peers?.deviceList?.forEach { item ->
                            Log.d(
                                TAG,
                                "device: ${item.deviceName}"
                            )
                        }

                        val ankitDevice =
                            peers?.deviceList?.find { item -> item.deviceName == "OnePlus 8T" }

                        if (!isConnected.get() && ankitDevice != null) {
                            binding.textView2.text = "status: P2P Ankit device found | connecting"

                            connectToGlass(ankitDevice)

                        }

                    }
                }
                WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                    binding.textView2.text = "status: P2P enabled | already connected"
                    if (manager == null) {
                        return
                    }
                    val networkInfo = intent
                        .getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO) as NetworkInfo?
                    if (networkInfo!!.isConnected) {

                        manager.requestConnectionInfo(channel, this@PhoneFragment)
                    } else {
                        // It's a disconnect
                        binding.textView2.text = "status: P2P enabled | disconnected"

                    }

                }
                WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION -> {
                    Log.d(TAG, "WIFI_P2P_THIS_DEVICE_CHANGED_ACTION: ")
                    // Respond to this device's wifi state changing
                }
            }
        }

    }

    override fun onConnectionInfoAvailable(info: WifiP2pInfo) {

    }
}