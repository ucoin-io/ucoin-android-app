package io.ucoin.app.fragment.wallet;

import android.app.Activity;
import android.app.DialogFragment;
import android.app.ListFragment;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import io.ucoin.app.Application;
import io.ucoin.app.R;
import io.ucoin.app.activity.IToolbarActivity;
import io.ucoin.app.activity.MainActivity;
import io.ucoin.app.adapter.WalletArrayAdapter;
import io.ucoin.app.model.local.Wallet;
import io.ucoin.app.model.remote.Currency;
import io.ucoin.app.service.ServiceLocator;
import io.ucoin.app.service.exception.DuplicatePubkeyException;
import io.ucoin.app.service.exception.PeerConnectionException;
import io.ucoin.app.service.exception.UidMatchAnotherPubkeyException;
import io.ucoin.app.service.local.WalletService;
import io.ucoin.app.technical.ExceptionUtils;
import io.ucoin.app.technical.task.AsyncTaskHandleException;
import io.ucoin.app.technical.task.NullAsyncTaskListener;
import io.ucoin.app.technical.task.ProgressDialogAsyncTaskListener;


public class WalletListFragment extends ListFragment implements MainActivity.QueryResultListener<Wallet>{

    private static final String TAG = "WalletListFragment";
    private static final String WALLET_LIST_ARGS_KEYS = "Wallets";

    private WalletArrayAdapter mWalletArrayAdapter;
    private WalletListListener mListener;
    private int mScrollState;
    private ListView mListView;

    public static WalletListFragment newInstance(WalletListListener listener) {
        WalletListFragment fragment = new WalletListFragment();
        fragment.setOnClickListener(listener);
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        // list adapter
        mWalletArrayAdapter = new WalletArrayAdapter(getActivity());
        setListAdapter(mWalletArrayAdapter);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        return inflater.inflate(R.layout.fragment_wallet_list,
                container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mListView = getListView();

        // Display the progress by default (onQuerySuccess will disable it)
        TextView v = (TextView) view.findViewById(android.R.id.empty);
        v.setVisibility(View.GONE);

        getListView().setOnScrollListener(new AbsListView.OnScrollListener() {

            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                mScrollState = scrollState;

            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {

            }

        });

        // Load wallets
        LoadWalletsTask loadWalletsTask = new LoadWalletsTask();
        loadWalletsTask.execute();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.toolbar_wallet_list, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_add_wallet:
                onAddWalletClick();
                return true;
            case R.id.action_refresh:
                onRefreshClick();
                return true;
        }

        return false;
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        if (mListener == null) {
            return;
        }
        Wallet wallet = (Wallet) l.getAdapter().getItem(position);
        Bundle bundle = new Bundle();
        bundle.putSerializable(Wallet.class.getSimpleName(), wallet);
        mListener.onPositiveClick(bundle);
    }


    public interface WalletListListener {
        public void onPositiveClick(Bundle args);
        public void onLoadFailed(Throwable t);
    }

    @Override
    public void onQuerySuccess(List<? extends Wallet> wallets) {

    }

    @Override
    public void onQueryFailed(String message) {
    }

    @Override
    public void onQueryCancelled(){
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    /* -- Internal methods -- */

    private void onRefreshClick(){
        int count = mWalletArrayAdapter.getCount();
        if (count == 0) {
            return;
        }

        // Copy wallet to array, from adapter
        List<Wallet> wallets = new ArrayList<Wallet>(count);
        for (int i = 0; i<count; i++) {
            wallets.add(mWalletArrayAdapter.getItem(i));
        }

        // Run wallets  update, using a progress dialog
        ServiceLocator.instance().getWalletService().updateWalletsRemotely(
                wallets,
                new ProgressDialogAsyncTaskListener<List<? extends Wallet>>(getActivity()) {

                    @Override
                    public void onProgressUpdate() {
                        // Update only when we're not scrolling, and only for visible views
                        if (mScrollState == AbsListView.OnScrollListener.SCROLL_STATE_IDLE) {
                            int start = mListView.getFirstVisiblePosition();
                            for (int i = start, j = mListView.getLastVisiblePosition(); i <= j; i++) {
                                View view = mListView.getChildAt(i - start);
                                Wallet wallet = (Wallet) mListView.getItemAtPosition(i);
                                if (wallet.isDirty()) {
                                    mListView.getAdapter().getView(i, view, mListView); // Tell the adapter to update this view
                                    wallet.setDirty(false);
                                }
                            }
                        }
                    }
                });
    }

    protected void onAddWalletClick() {
        Bundle args = getArguments();
        Currency currency = (Currency)args.getSerializable(Currency.class.getSimpleName());

        AddWalletDialogFragment.OnClickListener listener = new AddWalletDialogFragment.OnClickListener() {
            public void onPositiveClick(Bundle args) {

                Currency currency = (Currency)args.getSerializable(AddWalletDialogFragment.BUNDLE_CURRENCY);
                String alias = args.getString(AddWalletDialogFragment.BUNDLE_ALIAS);
                String uid = args.getString(AddWalletDialogFragment.BUNDLE_UID);
                String salt = args.getString(AddWalletDialogFragment.BUNDLE_SALT);
                String password = args.getString(AddWalletDialogFragment.BUNDLE_PASSWORD);

                WalletService walletService = ServiceLocator.instance().getWalletService();
                walletService.create(currency,
                        alias,
                        uid, salt, password,
                        new NullAsyncTaskListener<Wallet>(getActivity().getApplicationContext()) {

                            @Override
                            public void onSuccess(Wallet result) {
                                if (!getActivity().isDestroyed() && !getActivity().isFinishing()) {
                                    new LoadWalletsTask().execute();
                                }
                            }

                            @Override
                            public void onFailed(Throwable error) {
                                String errorMessage = ExceptionUtils.getMessage(error);
                                if (error instanceof DuplicatePubkeyException) {
                                    errorMessage = getString(R.string.duplicate_wallet_pubkey);
                                }
                                else if (error instanceof UidMatchAnotherPubkeyException) {
                                    errorMessage = getString(R.string.uid_match_another_pubkey);
                                }
                                else {
                                    Log.d(TAG, "Error in AddWalletTask", error);
                                }
                                Toast.makeText(getContext(),
                                        errorMessage,
                                        Toast.LENGTH_LONG).show();
                            }
                });
            }
        };
        DialogFragment fragment;
        if (currency == null) {
            fragment = AddWalletDialogFragment.newInstance(getActivity(), listener);
        }
        else {
            fragment = AddWalletDialogFragment.newInstance(currency, listener);
        }
        fragment.show(getFragmentManager(),
                fragment.getClass().getSimpleName());
    }

    private void setOnClickListener(WalletListListener listener) {
        mListener = listener;
    }

    public class LoadWalletsTask extends AsyncTaskHandleException<Void, Void, List<Wallet>> {

        private final long mAccountId;

        public LoadWalletsTask() {
            super(getActivity().getApplicationContext());
            mAccountId = ((Application)getActivity().getApplication()).getAccountId();

            ProgressDialog progressDialog = new ProgressDialog(getActivity());
            //progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            ProgressDialogAsyncTaskListener listener = new ProgressDialogAsyncTaskListener(progressDialog);
            setListener(listener);
        }

        @Override
        protected List<Wallet> doInBackgroundHandleException(Void... param) throws PeerConnectionException {
            ServiceLocator serviceLocator = ServiceLocator.instance();

            setMax(100);
            setProgress(0);

            // Load wallets
            return serviceLocator.getWalletService().getWalletsByAccountId(
                    getContext(),
                    mAccountId,
                    true,
                    LoadWalletsTask.this);
        }

        @Override
        protected void onSuccess(final List<Wallet> wallets) {
            mWalletArrayAdapter.setNotifyOnChange(true);
            mWalletArrayAdapter.clear();
            mWalletArrayAdapter.addAll(wallets);
            mWalletArrayAdapter.notifyDataSetChanged();
        }

        @Override
        protected void onFailed(Throwable t) {

            final String errorMessage = getString(R.string.connected_error, t.getMessage());
            Log.e(getClass().getSimpleName(), errorMessage, t);

            // Display the error
            Toast.makeText(getContext(),
                    errorMessage,
                    Toast.LENGTH_SHORT)
                    .show();

            if (mListener != null) {
                mListener.onLoadFailed(t);
            }
        }
    }

}
