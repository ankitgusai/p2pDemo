package com.ankit.p2pdemo

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.NetworkInfo
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.net.wifi.p2p.WifiP2pManager.ActionListener
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.ankit.p2pdemo.databinding.FragmentGlassesBinding
import java.io.IOException
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket


class GlassesFragment : Fragment(), WifiP2pManager.ConnectionInfoListener {
    lateinit var binding: FragmentGlassesBinding
    var channel: WifiP2pManager.Channel? = null
    lateinit var manager: WifiP2pManager
    val TAG = "Glasses frag"
    var hostAddress = ""


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentGlassesBinding.inflate(inflater, container, false)
        binding.textView.text = "status: initialising"

        manager = requireActivity().getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
        channel = manager.initialize(requireContext(), Looper.getMainLooper(), null)
        channel?.also { channel ->
        }

        binding.button3.setOnClickListener {
            sendFile(hostAddress)
        }
        return binding.root
    }

    fun discoverPeers() {
        manager.discoverPeers(channel, object : WifiP2pManager.ActionListener {

            override fun onSuccess() {
                Log.d(TAG, "onSuccess: ")
            }

            override fun onFailure(reasonCode: Int) {
                Log.d(TAG, "onFailure: ${reasonCode}")

            }
        })
    }

    fun sendFile(host: String) {
        Log.d(TAG, "sendFile: to ${host}")
        Thread(Runnable {

            val socket = Socket()
            val port = 8888
            try {
                socket.bind(null)
                socket.connect(InetSocketAddress(host, port), 5000)
                Log.d(TAG, "Client socket - " + socket.isConnected())
                val stream: OutputStream = socket.getOutputStream()
                requireActivity().assets
                    .open("temp.png").copyTo(stream, 512)

                Log.d(TAG, "Client: Data written")
            } catch (e: IOException) {
                e.message?.let { Log.e(TAG, it) }
            } finally {
                if (socket != null) {
                    if (socket.isConnected()) {
                        try {
                            socket.close()
                        } catch (e: IOException) {
                            // Give up
                            e.printStackTrace()
                        }
                    }
                }
            }
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
                            binding.textView.text = "status: P2P enabled | waiting to be connected"
                            discoverPeers()
                        }
                        else -> {
                            binding.textView.text = "status: P2P not enabled"
                        }
                    }
                }
                WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
                    Log.d(TAG, "WIFI_P2P_PEERS_CHANGED_ACTION: ")

                }
                WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                    binding.textView.text = "status: P2P connected | acquiring info"

                    if (manager == null) {
                        return
                    }
                    val networkInfo = intent
                        .getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO) as NetworkInfo?
                    if (networkInfo!!.isConnected) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                            manager.stopPeerDiscovery(channel, object: ActionListener{
                                override fun onSuccess() {
                                }

                                override fun onFailure(p0: Int) {
                                }
                            })
                        }
                        manager.requestConnectionInfo(channel, this@GlassesFragment)
                    } else {
                        // It's a disconnect
                        binding.textView.text = "status: P2P enabled | disconnected"
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
        Log.d(TAG, "onConnectionInfoAvailable: ${info.groupFormed} ${ info.isGroupOwner}")
        binding.button3.visibility = VISIBLE
        hostAddress = info.groupOwnerAddress.hostAddress
        if (info.groupFormed && info.isGroupOwner) {
            Log.d(TAG, "onConnectionInfoAvailable:  This says we are server , What the heck!!")
        } else if (info.groupFormed) {
            // This connection says we are glass, What the heck!!
            Log.d(TAG, "onConnectionInfoAvailable:  This connection says we are glass")
        }
    }

}
