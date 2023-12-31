package com.kaizenvpn.vpn.view;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.VpnService;
import android.os.Bundle;
import android.os.RemoteException;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.bumptech.glide.Glide;
import com.kaizenvpn.vpn.CheckInternetConnection;
import com.kaizenvpn.vpn.R;
import com.kaizenvpn.vpn.SharedPreference;
import com.kaizenvpn.vpn.databinding.FragmentMainBinding;
import com.kaizenvpn.vpn.interfaces.ChangeServer;
import com.kaizenvpn.vpn.model.Server;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import de.blinkt.openvpn.OpenVpnApi;
import de.blinkt.openvpn.core.OpenVPNService;
import de.blinkt.openvpn.core.OpenVPNThread;
import de.blinkt.openvpn.core.VpnStatus;

import static android.app.Activity.RESULT_OK;

public class MainFragment extends Fragment implements View.OnClickListener, ChangeServer {

    private Server server;
    private CheckInternetConnection connection;

    private OpenVPNThread vpnThread = new OpenVPNThread();
    private OpenVPNService vpnService = new OpenVPNService();
    boolean vpnStart = false;
    private SharedPreference preference;
    private FragmentMainBinding binding;
    ImageView powerButton;
    public TextView logTxt;
    private AdView mAdView;
    private InterstitialAd mInterstitialAd;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_main, container, false);

        View view = binding.getRoot();
        initializeAll();
        powerButton = view.findViewById(R.id.power_button);
        logTxt = view.findViewById(R.id.logTv);

        powerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (vpnStart) {
                    confirmDisconnect();
                } else {
                    prepareVpn();
                }

            }
        });


        // Ads

        MobileAds.initialize(getActivity(), new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) {
            }
        });

        //Interstitial Ads

        mInterstitialAd = new InterstitialAd(getActivity());
        mInterstitialAd.setAdUnitId(getString(R.string.interstitial_ad));
        mInterstitialAd.loadAd(new AdRequest.Builder().build());


        //Banner Ads
        mAdView = view.findViewById(R.id.banner);
        AdView adView = new AdView(getActivity());
        adView.setAdSize(AdSize.SMART_BANNER);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

        return view;
    }


    /**
     * Initialize all variable and object
     */
    private void initializeAll() {
        preference = new SharedPreference(getContext());
        server = preference.getServer();

        // Update current selected server icon
        updateCurrentServerIcon(server.getFlagUrl());

        connection = new CheckInternetConnection();
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(broadcastReceiver, new IntentFilter("connectionState"));
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.vpnBtn.setOnClickListener(this);

        // Checking is vpn already running or not
        isServiceRunning();
        VpnStatus.initLogCache(getActivity().getCacheDir());
    }

    /**
     * @param v: click listener view
     */
    @Override
    public void onClick(View v) {
        switch (v.getId()) {

            case R.id.vpnBtn:

                if (vpnStart) {
                    confirmDisconnect();
                } else {
                    prepareVpn();
                }
        }
    }

    /**
     * Show show disconnect confirm dialog
     */
    public void confirmDisconnect() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(getActivity().getString(R.string.connection_close_confirm));

        builder.setPositiveButton(getActivity().getString(R.string.yes), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                stopVpn();
            }
        });
        builder.setNegativeButton(getActivity().getString(R.string.no), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User cancelled the dialog
            }
        });

        // Create the AlertDialog
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    /**
     * Prepare for vpn connect with required permission
     */
    private void prepareVpn() {
        if (!vpnStart) {
            if (getInternetStatus()) {

                // Checking permission for network monitor
                Intent intent = VpnService.prepare(getContext());

                if (intent != null) {
                    startActivityForResult(intent, 1);
                } else startVpn();//have already permission

                // Update confection status
                status("connecting");

            } else {

                // No internet connection available
                showToast("you have no internet connection !!");
            }

        } else if (stopVpn()) {

            // VPN is stopped, show a Toast message.
            showToast("Disconnect Successfully");
        }
    }

    /**
     * Stop vpn
     *
     * @return boolean: VPN status
     */
    public boolean stopVpn() {
        try {
            vpnThread.stop();

            status("connect");
            vpnStart = false;
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Taking permission for network access
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {

            //Permission granted, start the VPN
            startVpn();
        } else {
            showToast("Permission Deny !! ");
        }
    }

    /**
     * Internet connection status.
     */
    public boolean getInternetStatus() {
        return connection.netCheck(getContext());
    }

    /**
     * Get service status
     */
    public void isServiceRunning() {
        setStatus(vpnService.getStatus());
    }

    /**
     * Start the VPN
     */
    private void startVpn() {
        try {
            // .ovpn file
            InputStream conf = getActivity().getAssets().open(server.getOvpn());
            InputStreamReader isr = new InputStreamReader(conf);
            BufferedReader br = new BufferedReader(isr);
            String config = "";
            String line;

            while (true) {
                line = br.readLine();
                if (line == null) break;
                config += line + "\n";
            }

            br.readLine();
            OpenVpnApi.startVpn(getContext(), config, server.getCountry(), server.getOvpnUserName(), server.getOvpnUserPassword());

            // Update log
            binding.logTv.setText("Connecting...");
            vpnStart = true;

        } catch (IOException | RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * Status change with corresponding vpn connection status
     *
     * @param connectionState
     */
    public void setStatus(String connectionState) {
        if (connectionState != null)
            switch (connectionState) {
                case "DISCONNECTED":
                    status("connect");
                    vpnStart = false;
                    vpnService.setDefaultStatus();

                    break;
                case "CONNECTED":
                    vpnStart = true;// it will use after restart this activity

                    if (mInterstitialAd.isLoaded()) {

                        mInterstitialAd.show();

                    }

                    status("connected");
                    binding.logTv.setText("Connected");
                    logTxt.setTextColor(getResources().getColor(R.color.colorPrimary));


                    break;
                case "WAIT":
                    binding.logTv.setText("waiting for server connection!!");
                    break;
                case "AUTH":
                    binding.logTv.setText("server authenticating!!");
                    break;
                case "RECONNECTING":
                    status("connecting");
                    binding.logTv.setText("Reconnecting...");
                    break;
                case "NONETWORK":
                    binding.logTv.setText("No network connection");
                    break;
            }

    }

    /**
     * Change button background color and text
     *
     * @param status: VPN current status
     */

    //If You are Using button then you can use setText() method
    public void status(String status) {

        if (status.equals("connect")) {

            powerButton.setImageResource(R.drawable.power_off);

            logTxt.setText(getString(R.string.not_connect));
            logTxt.setTextColor(getResources().getColor(R.color.red));


        } else if (status.equals("connecting")) {

            powerButton.setImageResource(R.drawable.power_connecting);

        } else if (status.equals("connected")) {

            powerButton.setImageResource(R.drawable.power_on);

        } else if (status.equals("tryDifferentServer")) {

            powerButton.setImageResource(R.drawable.power_on);

            // binding.vpnBtn.setText("Try Different\nServer");

        } else if (status.equals("loading")) {
            powerButton.setImageResource(R.drawable.power_off);

            // binding.vpnBtn.setText("Loading Server..");

        } else if (status.equals("invalidDevice")) {
            powerButton.setImageResource(R.drawable.power_on);

            // binding.vpnBtn.setText("Invalid Device");

        } else if (status.equals("authenticationCheck")) {

            powerButton.setImageResource(R.drawable.power_connecting);


            // binding.vpnBtn.setText("Authentication \n Checking...");
        }

    }

    /**
     * Receive broadcast message
     */
    BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                setStatus(intent.getStringExtra("state"));
            } catch (Exception e) {
                e.printStackTrace();
            }

            try {

                String duration = intent.getStringExtra("duration");
                String lastPacketReceive = intent.getStringExtra("lastPacketReceive");
                String byteIn = intent.getStringExtra("byteIn");
                String byteOut = intent.getStringExtra("byteOut");

                if (duration == null) duration = "00:00:00";
                if (lastPacketReceive == null) lastPacketReceive = "0";
                if (byteIn == null) byteIn = " ";
                if (byteOut == null) byteOut = " ";
                updateConnectionStatus(duration, lastPacketReceive, byteIn, byteOut);
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    };

    /**
     * Update status UI
     *
     * @param duration:          running time
     * @param lastPacketReceive: last packet receive time
     * @param byteIn:            incoming data
     * @param byteOut:           outgoing data
     */
    public void updateConnectionStatus(String duration, String lastPacketReceive, String byteIn, String byteOut) {
        binding.durationTv.setText(duration);
        binding.lastPacketReceiveTv.setText(lastPacketReceive + " sec.");

        //Speed Download
        binding.byteInTv.setText(byteIn);
        //Speed upload
        binding.byteOutTv.setText(byteOut);
    }

    /**
     * Show toast message
     *
     * @param message: toast message
     */
    public void showToast(String message) {
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
    }

    /**
     * VPN server country icon change
     *
     * @param serverIcon: icon URL
     */
    public void updateCurrentServerIcon(String serverIcon) {
        Glide.with(getContext())
                .load(serverIcon)
                .into(binding.connectedCountry);
    }

    /**
     * Change server when user select new server
     *
     * @param server ovpn server details
     */
    @Override
    public void newServer(Server server) {
        this.server = server;
        updateCurrentServerIcon(server.getFlagUrl());

        // Stop previous connection
        if (vpnStart) {
            stopVpn();
        }

        prepareVpn();
    }

    @Override
    public void onResume() {
        if (server == null) {
            server = preference.getServer();
        }
        super.onResume();
    }

    /**
     * Save current selected server on local shared preference
     */
    @Override
    public void onStop() {
        if (server != null) {
            preference.saveServer(server);
        }

        super.onStop();
    }
}
