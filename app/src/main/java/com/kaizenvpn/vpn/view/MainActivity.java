package com.kaizenvpn.vpn.view;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.kaizenvpn.vpn.R;

import com.kaizenvpn.vpn.interfaces.ChangeServer;
import com.kaizenvpn.vpn.interfaces.NavItemClickListener;
import com.kaizenvpn.vpn.model.Server;

import java.util.ArrayList;


public class MainActivity extends AppCompatActivity implements NavItemClickListener {
    private FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
    private Fragment fragment;
    private RecyclerView serverListRv;
    private ArrayList<Server> serverLists;
    
    private DrawerLayout drawer;
    private ChangeServer changeServer;

    

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize all variable
        initializeAll();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayShowTitleEnabled(false);


        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);

        
        transaction.add(R.id.container, fragment);
        transaction.commit();

        // Server List recycler view initialize
        
    }

    /**
     * Initialize all object, listener etc
     */
    private void initializeAll() {
        drawer = findViewById(R.id.drawer_layout);

        fragment = new MainFragment();
        serverListRv = findViewById(R.id.serverListRv);
        serverListRv.setHasFixedSize(true);

        serverListRv.setLayoutManager(new LinearLayoutManager(this));

        serverLists = getServerList();
        changeServer = (ChangeServer) fragment;

    }
 private void openYouTubeChannel() {
    String youtubeChannelUrl = "https://www.youtube.com/@kaizenvpn";
    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(youtubeChannelUrl));
    startActivity(browserIntent);
}

private void openFacebookPage() {
    String facebookPageUrl = "https://www.facebook.com/kaizenvpncom?notif_id=1701255007781978&notif_t=profile_plus_admin_invite&ref=notif";
    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(facebookPageUrl));
    startActivity(browserIntent);
}

private void privPolicy() {
    String privPolicyUrl = "https://www.kaizenvpn.com/privacy-policy";
    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(privPolicyUrl));
    startActivity(browserIntent);
}


    /**
     * Close navigation drawer
     */
    public void closeDrawer() {
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            drawer.openDrawer(GravityCompat.START);
        }
    }

    /**
     * Generate server array list
     */
    private ArrayList getServerList() {

        ArrayList<Server> servers = new ArrayList<>();

        servers.add(new Server("France",
                com.kaizenvpn.vpn.Utils.getImgURL(R.drawable.fr_flag),
                "tcp.ovpn"
        ));
        return servers;
    }


    /**
     * On navigation item click, close drawer and change server
     *
     * @param index: server index
     */
    @Override
    public void clickedItem(int index) {
        closeDrawer();
        changeServer.newServer(serverLists.get(index));
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);


        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        switch (item.getItemId()) {

            case R.id.share:

                Intent sharingIntent = new Intent(Intent.ACTION_SEND);
                sharingIntent.setType("text/plain");
                sharingIntent.putExtra(Intent.EXTRA_TEXT, "KaizenVPN" + getString(R.string.app_name) + "* Save Your Day : https://kaizenvpn.com");
                sharingIntent.putExtra(Intent.EXTRA_SUBJECT, "Subject");
                startActivity(Intent.createChooser(sharingIntent, "Share using"));

                break;

            case R.id.about:

                Toast.makeText(this, "Created by KaizenVPN", Toast.LENGTH_SHORT).show();

                break;
            case R.id.youtubeChannel:
            openYouTubeChannel();
            break;
            
            case R.id.openFacebookPage:
            openFacebookPage();
            break;

            case R.id.privPolicy:
            privPolicy();
            break;

        }


        return super.onOptionsItemSelected(item);
    }

}
