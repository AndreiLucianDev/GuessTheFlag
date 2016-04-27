package app.com.dev.andreilucian.guesstheflag;

import android.os.Bundle;
import android.preference.PreferenceFragment;

/**
 * A placeholder fragment containing a simple view.
 */
public class SettingsActivityFragment extends PreferenceFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //add the preferences loaded from the xml file
        addPreferencesFromResource(R.xml.preferences);
    }
}
