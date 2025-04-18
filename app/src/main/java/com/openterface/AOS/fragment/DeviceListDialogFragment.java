/**
* @Title: DeviceListDialogFragment
* @Package com.openterface.AOS.fragment
* @Description:
 * ========================================================================== *
 *                                                                            *
 *    This file is part of the Openterface Mini KVM App Android version       *
 *                                                                            *
 *    Copyright (C) 2024   <info@openterface.com>                             *
 *                                                                            *
 *    This program is free software: you can redistribute it and/or modify    *
 *    it under the terms of the GNU General Public License as published by    *
 *    the Free Software Foundation version 3.                                 *
 *                                                                            *
 *    This program is distributed in the hope that it will be useful, but     *
 *    WITHOUT ANY WARRANTY; without even the implied warranty of              *
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU        *
 *    General Public License for more details.                                *
 *                                                                            *
 *    You should have received a copy of the GNU General Public License       *
 *    along with this program. If not, see <http://www.gnu.org/licenses/>.    *
 *                                                                            *
 * ========================================================================== *
*/
package com.openterface.AOS.fragment;

import android.app.Dialog;
import android.hardware.usb.UsbDevice;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.openterface.AOS.ICameraHelper;
import com.openterface.AOS.R;
import com.openterface.AOS.adapter.DeviceItemRecyclerViewAdapter;
import com.openterface.AOS.databinding.FragmentDeviceListBinding;

import java.lang.ref.WeakReference;
import java.util.List;

public class DeviceListDialogFragment extends DialogFragment {

    private WeakReference<ICameraHelper> mCameraHelperWeak;
    private UsbDevice mUsbDevice;

    private OnDeviceItemSelectListener mOnDeviceItemSelectListener;

    private FragmentDeviceListBinding mBinding;

    public DeviceListDialogFragment(ICameraHelper cameraHelper, UsbDevice usbDevice) {
        mCameraHelperWeak = new WeakReference<>(cameraHelper);
        mUsbDevice = usbDevice;
    }

    public DeviceListDialogFragment() {
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        mBinding = FragmentDeviceListBinding.inflate(getLayoutInflater());
        initDeviceList();

        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        builder.setTitle(R.string.device_list_dialog_title);
        builder.setView(mBinding.getRoot());
        builder.setNegativeButton(R.string.device_list_cancel_button, (dialog, which) -> {
            dismiss();
        });
        return builder.create();
    }

    private void initDeviceList() {
        if (mCameraHelperWeak.get() != null) {
            List<UsbDevice> list = mCameraHelperWeak.get().getDeviceList();
            if (list == null || list.size() == 0) {
                mBinding.rvDeviceList.setVisibility(View.GONE);
                mBinding.tvEmptyTip.setVisibility(View.VISIBLE);
            } else {
                mBinding.rvDeviceList.setVisibility(View.VISIBLE);
                mBinding.tvEmptyTip.setVisibility(View.GONE);

                DeviceItemRecyclerViewAdapter adapter = new DeviceItemRecyclerViewAdapter(list, mUsbDevice);
                mBinding.rvDeviceList.setAdapter(adapter);

                adapter.setOnItemClickListener((itemView, position) -> {
                    if (mOnDeviceItemSelectListener != null) {
                        mOnDeviceItemSelectListener.onItemSelect(list.get(position));
                    }
                    dismiss();
                });
            }
        }
    }

    public void setOnDeviceItemSelectListener(OnDeviceItemSelectListener listener) {
        mOnDeviceItemSelectListener = listener;
    }

    public interface OnDeviceItemSelectListener {
        void onItemSelect(UsbDevice usbDevice);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(false);
    }

    @Override
    public void onStart() {
        super.onStart();
        // Make sure the dialog box is displayed correctly when the Activity is restored
        Dialog dialog = getDialog();
        if (dialog != null) {
            dialog.setOnDismissListener(dialog1 -> {
                if (mOnDeviceItemSelectListener != null) {
                    mOnDeviceItemSelectListener = null;
                }
            });
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        // Clear the resources when it stops
        if (mBinding != null) {
            mBinding = null;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mBinding != null) {
            mBinding = null;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mOnDeviceItemSelectListener != null) {
            mOnDeviceItemSelectListener = null;
        }
        if (mCameraHelperWeak != null) {
            mCameraHelperWeak.clear();
            mCameraHelperWeak = null;
        }
        mUsbDevice = null;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mOnDeviceItemSelectListener = null;
    }

    public static DeviceListDialogFragment newInstance(ICameraHelper cameraHelper, UsbDevice usbDevice) {
        DeviceListDialogFragment fragment = new DeviceListDialogFragment();
        fragment.mCameraHelperWeak = new WeakReference<>(cameraHelper);
        fragment.mUsbDevice = usbDevice;
        return fragment;
    }
}
