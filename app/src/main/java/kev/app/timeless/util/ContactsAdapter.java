package kev.app.timeless.util;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import kev.app.timeless.R;
import kev.app.timeless.model.Contacto;

public class ContactsAdapter extends ListAdapter<Contacto, ContactsAdapter.ViewHolder> {
    private View.OnLongClickListener onLongClickListener;

    public ContactsAdapter(@NonNull DiffUtil.ItemCallback<Contacto> diffCallback, View.OnLongClickListener onLongClickListener) {
        super(diffCallback);
        this.onLongClickListener = onLongClickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ContactsAdapter.ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.contact, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Contacto contacto = getItem(position);

        if (contacto == null) {
            return;
        }

        holder.contacto.setText(String.valueOf(contacto.getNrTelefone()));
    }

    @Override
    public void onViewAttachedToWindow(@NonNull ViewHolder holder) {
        super.onViewAttachedToWindow(holder);
        holder.contacto.setOnLongClickListener(onLongClickListener);
    }

    @Override
    public void onViewDetachedFromWindow(@NonNull ViewHolder holder) {
        super.onViewDetachedFromWindow(holder);
        holder.contacto.setOnLongClickListener(null);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        protected TextView contacto;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            contacto = itemView.findViewById(R.id.contacto);
        }

        public String getNrTelefone () {
            return (String) contacto.getText();
        }
    }
}