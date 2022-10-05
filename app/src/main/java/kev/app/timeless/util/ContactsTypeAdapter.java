package kev.app.timeless.util;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.checkbox.MaterialCheckBox;

import kev.app.timeless.R;

public class ContactsTypeAdapter extends ListAdapter<String, ContactsTypeAdapter.ViewHolder> {
    private final CompoundButton.OnCheckedChangeListener onCheckedChangeListener;

    public ContactsTypeAdapter(@NonNull DiffUtil.ItemCallback<String> diffCallback, CompoundButton.OnCheckedChangeListener onCheckedChangeListener) {
        super(diffCallback);
        this.onCheckedChangeListener = onCheckedChangeListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ContactsTypeAdapter.ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.choice_type_contact, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String text = getItem(position);

        if (TextUtils.isEmpty(text)) {
            return;
        }

        holder.materialCheckBox.setText(text);
    }

    @Override
    public void onViewAttachedToWindow(@NonNull ViewHolder holder) {
        super.onViewAttachedToWindow(holder);
        holder.materialCheckBox.setOnCheckedChangeListener(onCheckedChangeListener);
    }

    @Override
    public void onViewDetachedFromWindow(@NonNull ViewHolder holder) {
        super.onViewDetachedFromWindow(holder);
        holder.materialCheckBox.setOnCheckedChangeListener(null);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final MaterialCheckBox materialCheckBox;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            materialCheckBox = itemView.findViewById(R.id.escolha);
        }
    }
}