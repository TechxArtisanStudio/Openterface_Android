package com.openterface.AOS.fragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.openterface.AOS.R;
import com.openterface.AOS.manager.LocaleManager;

/**
 * Dialog for picking the app language/locale.
 * When the user selects a language, LocaleManager.setLocale() is called to save the preference,
 * and AppCompatDelegate.setApplicationLocales() automatically recreates all activities.
 */
public class LanguagePickerDialogFragment extends DialogFragment {

    private static final String TAG = "LangPicker";

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        LocaleManager localeManager = LocaleManager.getInstance();
        String[][] locales = LocaleManager.getAvailableLocales();

        // Build display names array
        String[] displayNames = new String[locales.length];
        for (int i = 0; i < locales.length; i++) {
            displayNames[i] = locales[i][1]; // Use the full display name with language in native form
        }

        int currentIndex = localeManager.getCurrentLocaleIndex();

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle(R.string.language)
                .setSingleChoiceItems(displayNames, currentIndex, (dialog, which) -> {
                    // Save the selected locale preference
                    String selectedCode = locales[which][0];
                    Log.d(TAG, "Language selected: index=" + which + ", code=" + selectedCode + ", display=" + displayNames[which]);
                    localeManager.setLocale(requireContext(), selectedCode);
                    dialog.dismiss();

                    // Explicitly recreate the activity to apply the new locale
                    if (getActivity() != null) {
                        Log.d(TAG, "Recreating activity to apply locale change");
                        getActivity().recreate();
                    }
                })
                .setNegativeButton(R.string.permission_cancel, (dialog, which) -> dialog.dismiss());

        return builder.create();
    }
}