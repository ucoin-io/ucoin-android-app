package org.duniter.app.view;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.duniter.app.AppPreferences;
import org.duniter.app.Application;
import org.duniter.app.Format;
import org.duniter.app.R;
import org.duniter.app.model.Entity.BlockUd;
import org.duniter.app.model.Entity.Contact;
import org.duniter.app.model.Entity.Currency;
import org.duniter.app.model.EntityServices.BlockService;
import org.duniter.app.model.EntityServices.CurrencyService;
import org.duniter.app.model.EntityServices.IdentityService;
import org.duniter.app.services.SqlService;
import org.duniter.app.technical.callback.CallbackBlock;
import org.duniter.app.technical.callback.CallbackLookup;
import org.duniter.app.technical.format.Contantes;
import org.duniter.app.view.currency.RulesBisFragment;
import org.duniter.app.view.identity.CertificationFragment;
import org.duniter.app.view.identity.IdentityFragment;
import org.duniter.app.view.identity.IdentityListFragment;
import org.duniter.app.view.currency.BlockListFragment;
import org.duniter.app.view.currency.RulesFragment;
import org.duniter.app.view.currency.SourceListFragment;
import org.duniter.app.view.wallet.WalletListFragment;

/**
 * Created by naivalf27 on 19/04/16.
 */
public class MainActivity extends ActionBarActivity implements View.OnClickListener {

    private Context ctx;

    private ActionBarDrawerToggle mToggle;
    private DrawerLayout mDrawerLayout;
    private DrawerLayout mDrawerContact;
    private TextView drawerContactsView;
    private TextView drawerWalletsView;
    private TextView drawerRulesView;
    private TextView drawerBlocksView;
    private TextView drawerSourcesView;
    private TextView drawerCreditView;
    private TextView drawerDbView;
    private Fragment currentFragment;
    private ArrayList<Fragment> listFragment = null;
    public static int RESULT_SCAN = 49374;

    private static Long wId;

    private Currency currency;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_currency);
        ctx = this;

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        try {
            setSupportActionBar(toolbar);
        } catch (Throwable t) {
            Log.w("setSupportActionBar", t.getMessage());
        }

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

        findViewById(R.id.deconnection).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deconnection(true);
                Application.cancelSync();
            }
        });
        findViewById(R.id.request_sync).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Application.forcedSync();
                closeDrawer();
                Toast.makeText(ctx,getString(R.string.synchro),Toast.LENGTH_LONG).show();
            }
        });

        initDrawer();
        mToggle = new ActionBarDrawerToggle(this, mDrawerLayout,
                R.string.open_drawer, R.string.close_drawer);

        long currencyId = preferences.getLong(Application.CURRENCY_ID,0);
        if (currencyId==0){
            currency = SqlService.getCurrencySql(this).getAllCurrency().get(0);
        }else{
            currency = SqlService.getCurrencySql(this).getById(currencyId);
        }

        CurrencyService.updateCurrency(this, currency, null);

        if(listFragment == null){
            listFragment = new ArrayList<>();
        }

        if(currentFragment == null){
            setCurrentFragment(WalletListFragment.newInstance(currency, true));
        }else{
            displayFragment(currentFragment,false);
        }

//        if (savedInstanceState == null){
//            displayListWalletFragment();
//        }
        updateDrawer();

        preferences.registerOnSharedPreferenceChangeListener(new SharedPreferences.OnSharedPreferenceChangeListener() {
            public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
                switch (key){
                    case Application.UNIT:
                    case Application.UNIT_DEFAULT:
                    case Application.DECIMAL:
                    case Application.USE_OBLIVION:
                    case Application.DISPLAY_DU:
                        updateFragment();
                        break;
                    case Application.DELAY_SYNC:
                        Application.forcedSync();
                        break;
                }
            }
        });
    }

    private void updateFragment() {
        currentFragment.onResume();
    }

    private void initDrawer(){
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawerWalletsView = (TextView) mDrawerLayout.findViewById(R.id.drawer_wallets);
        drawerContactsView = (TextView) mDrawerLayout.findViewById(R.id.drawer_contacts);
        TextView drawerPeersView = (TextView) mDrawerLayout.findViewById(R.id.drawer_peers);
        TextView drawerCurrencyView = (TextView) mDrawerLayout.findViewById(R.id.drawer_currency);
        TextView drawerSettingsView = (TextView) mDrawerLayout.findViewById(R.id.drawer_settings);
        drawerCreditView = (TextView) mDrawerLayout.findViewById(R.id.drawer_credits);
        drawerRulesView = (TextView) mDrawerLayout.findViewById(R.id.drawer_rules);
        drawerBlocksView = (TextView) mDrawerLayout.findViewById(R.id.drawer_blocks);
        drawerSourcesView = (TextView) mDrawerLayout.findViewById(R.id.drawer_sources);
        drawerDbView = (TextView) mDrawerLayout.findViewById(R.id.export_db);

        drawerRulesView.setOnClickListener(this);
        drawerWalletsView.setOnClickListener(this);
        drawerContactsView.setOnClickListener(this);
        drawerCurrencyView.setOnClickListener(this);
        drawerSettingsView.setOnClickListener(this);
        drawerCreditView.setOnClickListener(this);

        if (false) {
            drawerPeersView.setVisibility(View.VISIBLE);
            drawerPeersView.setOnClickListener(this);

            drawerBlocksView.setVisibility(View.VISIBLE);
            drawerBlocksView.setOnClickListener(this);

            drawerSourcesView.setVisibility(View.VISIBLE);
            drawerSourcesView.setOnClickListener(this);

            drawerDbView.setVisibility(View.VISIBLE);
            drawerDbView.setOnClickListener(this);
        }
    }

    public void verifyUpdate(){
        final Context context = this;
        BlockService.getCurrentBlock(context, this.currency, new CallbackBlock() {
            @Override
            public void methode(BlockUd blockUd) {
                if(currency.getCurrentBlock().getNumber()< blockUd.getNumber()){
                    CurrencyService.updateCurrency(context,currency,null);
                    currency.setCurrentBlock(blockUd);
                    setCurrency(currency);
                }
            }
        });
    }

    public void updateDrawer() {
        TextView drawerCurrencyName = (TextView) findViewById(R.id.drawer_currency_name);
        drawerCurrencyName.setText(this.currency.getName());

        if(this.currency.getCurrentBlock()==null){
            BlockService.getCurrentBlock(this, this.currency, new CallbackBlock() {
                @Override
                public void methode(BlockUd blockUd) {
                    currency.setCurrentBlock(blockUd);
                    setCurrency(currency);
                }
            });
        }else{
            setCurrency(this.currency);
        }
    }

    @Override
    public void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mToggle.syncState();
    }

    private void setCurrency(Currency currency){
        this.currency = currency;
        TextView drawerMembersCount = (TextView) findViewById(R.id.drawer_members_count);
        TextView drawerBlockNumber = (TextView) findViewById(R.id.drawer_block_number);
        TextView drawerDate = (TextView) findViewById(R.id.drawer_date);

        drawerMembersCount.setText(
                this.currency.getCurrentBlock().getMembersCount() +
                        " " +
                        getResources().getString(R.string.members));

        drawerBlockNumber.setText(
                getResources().getString(R.string.block) +
                        " #" +
                        this.currency.getCurrentBlock().getNumber());

        Date d = new Date(this.currency.getCurrentBlock().getMedianTime() * 1000);
        drawerDate.setText(new SimpleDateFormat("EEE dd MMM yyyy hh:mm:ss").format(d));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mToggle.onOptionsItemSelected(item))
            return true;

        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return false;
    }

    @Override
    public void onBackPressed() {
        if (mDrawerLayout.isDrawerOpen(Gravity.START)) {
            closeDrawer();
            return;
        }

        if(listFragment.size()==1){
            askQuitApplication();
        }else if(listFragment.size()>1){
            listFragment.remove(listFragment.size()-1);
            currentFragment = listFragment.get(listFragment.size()-1);
            activeDrawer();
            displayFragment(currentFragment,true);
        }
    }

    private void activeDrawer(){
        if (currentFragment instanceof WalletListFragment){
            drawerWalletsView.setActivated(true);
            drawerContactsView.setActivated(false);
            drawerRulesView.setActivated(false);
            drawerCreditView.setActivated(false);
        }else if(currentFragment instanceof IdentityListFragment){
            drawerWalletsView.setActivated(false);
            drawerContactsView.setActivated(true);
            drawerRulesView.setActivated(false);
            drawerCreditView.setActivated(false);
        }else if(currentFragment instanceof RulesFragment){
            drawerWalletsView.setActivated(false);
            drawerContactsView.setActivated(false);
            drawerRulesView.setActivated(true);
            drawerCreditView.setActivated(false);
        }else if(currentFragment instanceof CreditFragment){
            drawerWalletsView.setActivated(false);
            drawerContactsView.setActivated(false);
            drawerRulesView.setActivated(false);
            drawerCreditView.setActivated(true);
        }
    }

    public void askQuitApplication(){
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setTitle("");
        alertDialogBuilder
                .setMessage("Do you want to exit the application ?")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        quit();
                    }
                }).setNegativeButton("No", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    private void quit(){
        PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
                .edit()
                .putBoolean(Application.CONNECTED, false)
                .apply();
        super.onBackPressed();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (resultCode == RESULT_OK){
            if (requestCode == RESULT_SCAN){
                IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
                Map<String, String> data = Format.parseUri(scanResult.getContents());
                Currency currency;

                final String uid = Format.isNull(data.get(Contantes.UID));
                final String publicKey = Format.isNull(data.get(Contantes.PUBLICKEY));
                final String currencyName = Format.isNull(data.get(Contantes.CURRENCY));

                if (currencyName.length()!=0){
                    currency = SqlService.getCurrencySql(this).getByName(currencyName);
                    if (currency == null){
                        Toast.makeText(this,getString(R.string.dont_know_currency),Toast.LENGTH_LONG).show();
                    }
                }else{
                    if(this.currency == null){
                        Toast.makeText(this,getString(R.string.contact_not_correct_currency),Toast.LENGTH_LONG).show();
                        currency = null;
                    }else{
                        currency = this.currency;
                    }
                }

                if (currency != null) {
                    IdentityService.getIdentity(this, currency, publicKey, new CallbackLookup() {
                        @Override
                        public void methode(List<Contact> contactList) {
                            if (contactList.size() != 0) {
                                if (uid.length() == 0 || uid.equals(contactList.get(0).getUid())) {
                                    Contact contact = contactList.get(0);
                                    Bundle args = new Bundle();
                                    args.putSerializable(Application.CONTACT, contact);
                                    setCurrentFragment(IdentityFragment.newInstance(args));
                                }else{
                                    Toast.makeText(ctx,ctx.getString(R.string.found_issue_qrcode),Toast.LENGTH_LONG).show();
                                }
                            }else{
                                Toast.makeText(ctx,getString(R.string.no_identity_found),Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }
            }
        }else{
            return;
        }
    }

    public void setDrawerIndicatorEnabled(final boolean enabled) {
        if (mToggle.isDrawerIndicatorEnabled() == enabled) {
            return;
        }

        float start = enabled ? 1f : 0f;
        float end = Math.abs(start - 1);
        ValueAnimator offsetAnimator = ValueAnimator.ofFloat(start, end);
        offsetAnimator.setDuration(300);
        //offsetAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        offsetAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float offset = (Float) animation.getAnimatedValue();
                mToggle.onDrawerSlide(null, offset);
            }
        });

        offsetAnimator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                if (enabled) {
                    mToggle.setDrawerIndicatorEnabled(enabled);
                }
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (!enabled) {
                    mToggle.setDrawerIndicatorEnabled(enabled);
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        offsetAnimator.start();
    }

//    public void clearAllFragments() {
//        getFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
//        setTitle(R.string.app_name);
//    }

    public void closeDrawer() {
        mDrawerLayout.closeDrawer(findViewById(R.id.drawer_panel));
    }

    @Override
    protected void onStop() {
        deconnection(false);
        super.onStop();
    }

    @Override
    public void onClick(View v) {
        closeDrawer();

        switch (v.getId()) {
            case R.id.drawer_wallets:
                removeList(true);
                setCurrentFragment(WalletListFragment.newInstance(currency, true));
                break;
            case R.id.drawer_contacts:
                removeList(false);
                setCurrentFragment(IdentityListFragment.newInstance(currency,"",false,false,false,true,true));
                break;
//            case R.id.drawer_currency:
//                removeList(false);
//                displayListCurrencyFragment();
//                break;
            case R.id.drawer_rules:
                removeList(false);
//                setCurrentFragment(RulesFragment.newInstance(currency));
                setCurrentFragment(RulesBisFragment.newInstance(currency));
                break;
//            case R.id.drawer_peers:
//                removeList(false);
//                displayListPeerFragment();
//                break;
            case R.id.drawer_blocks:
                removeList(false);
                setCurrentFragment(BlockListFragment.newInstance(currency.getId()));
                break;
            case R.id.drawer_sources:
                removeList(false);
                setCurrentFragment(SourceListFragment.newInstance(currency.getId()));
                break;
            case R.id.drawer_settings:
                Intent i = new Intent(MainActivity.this, AppPreferences.class);
                startActivity(i);
                break;
            case R.id.drawer_credits:
                removeList(false);
                setCurrentFragment(CreditFragment.newInstance());
                break;
            case R.id.export_db:
                Application.exportDb();
                break;
        }
    }

    public void setCurrentFragment(Fragment fragment){
        currentFragment = fragment;
        activeDrawer();
        addFragment();
        displayFragment(currentFragment,false);
    }

//    private void displayListCurrencyFragment(){
//        currentFragment = CurrencyListFragment.newInstance();
//        addFragment();
//        displayFragment(currentFragment);
//    }


    private void deconnection(boolean total){
        PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
                .edit()
                .putBoolean(Application.CONNECTED,false).apply();
        if(total) {
            Intent intent = new Intent(MainActivity.this, InitActivity.class);
            startActivity(intent);
            finish();
        }
    }

    private void displayFragment(Fragment fragment,boolean isBack){
        FragmentManager fragmentManager = getFragmentManager();

        int[] anim = new int[4];

        if(fragment instanceof CertificationFragment){
            anim[0] = R.animator.slide_in_up;
            anim[1] = R.animator.none_out;
            anim[2] = R.animator.none_in;
            anim[3] = R.animator.slide_out_up;
        }else{
            anim[0] = R.animator.slide_in_right;
            anim[1] = R.animator.slide_out_left;
            anim[2] = R.animator.slide_in_left;
            anim[3] = R.animator.slide_out_right;
        }

        if (isBack){
            fragmentManager.popBackStack();
        }else{
            fragmentManager.beginTransaction()
                    .setCustomAnimations(
                            anim[0],
                            anim[1],
                            anim[2],
                            anim[3])
                    .replace(R.id.frame_content, fragment, fragment.getClass().getSimpleName())
                    .addToBackStack(fragment.getClass().getSimpleName())
                    .commit();
            closeDrawer();
        }


//        .setCustomAnimations(
//                isBack ? anim[0] : anim[1],
//                isBack ? anim[2] : anim[3],
//                isBack ? anim[4] : anim[5],
//                isBack ? anim[6] : anim[7])

//        fragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
//        fragmentManager.beginTransaction()
//                .addToBackStack(fragment.getClass().getSimpleName())
//                .replace(R.id.frame_content, fragment, fragment.getClass().getSimpleName())
//                .commit();
        // close the drawer

    }

    private void addFragment(){
        if(listFragment.size()==0){
            listFragment.add(currentFragment);
        }else if (listFragment.get(listFragment.size()-1) != currentFragment){
            listFragment.add(currentFragment);
        }
    }

    private void removeList(boolean debut){
        if(debut){
            listFragment.clear();
        }else {
            Fragment f =listFragment.get(0);
            listFragment.clear();
            listFragment.add(f);
        }
    }
}