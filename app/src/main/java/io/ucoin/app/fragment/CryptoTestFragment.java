package io.ucoin.app.fragment;

import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import io.ucoin.app.R;
import io.ucoin.app.service.CryptoService;
import io.ucoin.app.service.ServiceLocator;
import io.ucoin.app.service.WotService;
import io.ucoin.app.technical.AsyncTaskHandleException;
import io.ucoin.app.technical.crypto.CryptoUtils;
import io.ucoin.app.technical.crypto.TestFixtures;

public class CryptoTestFragment extends Fragment {

    private TextView resultText;


    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        return inflater.inflate(R.layout.fragment_crypto_test,
                container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        getActivity().setTitle(getString(R.string.crypto_test));

        resultText = (TextView) view.findViewById(R.id.resultText);

        Button seedButton = (Button) view.findViewById(R.id.generateButton);
        seedButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                generate();
            }
        });

        Button signButton = (Button) view.findViewById(R.id.signButton);
        signButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sign();
            }
        });

        Button selfButton = (Button) view.findViewById(R.id.selfButton);
        selfButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                self();
            }
        });
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);
    }

    private void generate() {
        TestFixtures fixtures = new TestFixtures();
        resultText.setText("waiting...");
        try {

            String salt = fixtures.getUserSalt();
            String password = fixtures.getUserPassword();
            String expectedBase64Hash = fixtures.getUserSeedHash();

            CryptoService service = ServiceLocator.instance().getCryptoService();

            byte[] seed = service.computeSeed(salt, password);
            String hash = CryptoUtils.encodeBase64(seed);

            boolean isSuccess =  isEquals(expectedBase64Hash, hash);

            //TweetNaCl.KeyPair kp = new TweetNaCl.KeyPair();
            resultText.setText("result: " + isSuccess);
        }
        catch (Exception e) {
            resultText.setText(e.getMessage());
            Log.e("CryptoTestActivity", e.getMessage(), e);
        }

    }

    private void sign() {
        TestFixtures fixtures = new TestFixtures();
        resultText.setText("waiting...");
        try {

            String rawPub = fixtures.getUserPublicKey();
            String rawSec = fixtures.getUserPrivateKey();
            byte[] pub = CryptoUtils.decodeBase58(rawPub);
            byte[] sec = CryptoUtils.decodeBase58(rawSec);
            String rawMsg = "UID:"+fixtures.getUid()+"\n"
                    + "META:TS:1420881879\n";
            String rawSig = "TMgQysT7JwY8XwemskwWb8LBDJybLUsnxqaaUvSteIYpOxRiB92gkFQQcGpBwq4hAwhEiqBAiFkiXIozppDDDg==";

            CryptoService service = ServiceLocator.instance().getCryptoService();
            String signature = service.sign(rawMsg, sec);

            boolean isSuccess =  isEquals(rawSig, signature);

            resultText.setText("result: " + isSuccess);
        }
        catch (Exception e) {
            resultText.setText(e.getMessage());
            Log.e("CryptoTestActivity", e.getMessage(), e);
        }

    }

    private void self() {
        TestFixtures fixtures = new TestFixtures();
        resultText.setText("waiting...");

        String rawPub = fixtures.getUserPublicKey();
        String rawSec = fixtures.getUserPrivateKey();
        String uid = fixtures.getUid();
        byte[] pub = CryptoUtils.decodeBase58(rawPub);
        byte[] sec = CryptoUtils.decodeBase58(rawSec);

        SendSelfTask task = new SendSelfTask(pub, sec, uid, 1420881879);
        task.execute((Void) null);
    }

    protected static boolean isEquals(byte[] expectedData, byte[] actualData) {
        if (expectedData == null && actualData != null) {
            return false;
        }

        if (expectedData != null && actualData == null) {
            return false;
        }

        return expectedData.equals(actualData);
    }

    protected static boolean isEquals(String expectedData, String actualData) {
        if (expectedData == null && actualData != null) {
            return false;
        }

        if (expectedData != null && actualData == null) {
            return false;
        }

        return expectedData.equals(actualData);
    }

    /**
     * Represents an asynchronous task used to send self certification
     * the user.
     */
    public class SendSelfTask extends AsyncTaskHandleException<Void, Void, String> {

        private final String mUid;
        private final byte[] mPubKey;
        private final byte[] mSecKey;
        private final long mTimestamp;

        SendSelfTask(byte[] pubKey, byte[] secKey, String uid, long timestamp) {
            mPubKey = pubKey;
            mSecKey = secKey;
            mUid = uid;
            mTimestamp = timestamp;
        }

        @Override
        protected String doInBackgroundHandleException(Void... params) {

            WotService service = ServiceLocator.instance().getWotService();
            return service.sendSelf(mPubKey, mSecKey, mUid);
        }

        @Override
        protected void onSuccess(String result) {
            if (result == null || result.trim().length() == 0) {
                result = "successfully send self";
            }
            resultText.setText(result);
        }

        @Override
        protected void onFailed(Throwable t) {
            resultText.setText(t.getMessage());
            Log.e("CryptoTestActivity", t.getMessage(), t);
        }

        @Override
        protected void onCancelled() {
        }
    }
}