package kev.app.timeless.util;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.text.PrecomputedTextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textview.MaterialTextView;

import java.util.concurrent.Executor;

import kev.app.timeless.R;
import kev.app.timeless.model.Contacto;

public class ContactsAdapter extends ListAdapter<Contacto,  RecyclerView.ViewHolder> {
    private final View.OnClickListener onClickListener;
    private final Executor executor;

    public ContactsAdapter(@NonNull DiffUtil.ItemCallback<Contacto> diffCallback, View.OnClickListener onClickListener, Executor executor) {
        super(diffCallback);
        this.onClickListener = onClickListener;
        this.executor = executor;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        switch (holder.getItemViewType()) {
            case 0: adicionarNrTelefone(holder.itemView, position);
                break;
            case 1: adicionarText(holder.itemView, position);
                break;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return viewType == 0 ? new ContactsAdapter.ContactViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.contact, parent, false)) : new ContactsAdapter.TypeContactsViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.type_contacts, parent, false));
    }

    @Override
    public int getItemViewType(int position) {
        return getItem(position) == null ? 1 : super.getItemViewType(position);
    }

    @Override
    public void onViewAttachedToWindow(@NonNull RecyclerView.ViewHolder holder) {
        super.onViewAttachedToWindow(holder);
        if (holder.getItemViewType() != 0) {
            return;
        }

        holder.itemView.findViewById(R.id.more).setOnClickListener(onClickListener);
    }

    @Override
    public void onViewDetachedFromWindow(@NonNull RecyclerView.ViewHolder holder) {
        super.onViewDetachedFromWindow(holder);
        if (holder.getItemViewType() != 0) {
            return;
        }

        holder.itemView.findViewById(R.id.more).setOnClickListener(null);
    }

    private void adicionarNrTelefone(View itemView, int position) {
        MaterialTextView materialTextView = itemView.findViewById(R.id.txtContacto);

        Contacto contacto;

        try {
            contacto = getItem(position);
        } catch (Exception e) {
            return;
        }

        try {
            materialTextView.setTextFuture(PrecomputedTextCompat.getTextFuture(String.valueOf(contacto.getNrTelefone()), materialTextView.getTextMetricsParamsCompat(), null));
        } catch (Exception e) {
            materialTextView.setText(String.valueOf(contacto.getNrTelefone()));
        }
    }

    private void adicionarText(View itemView, int position) {
        Contacto contacto;

        try {
            contacto = getItem(position + 1);
        } catch (Exception e) {
            return;
        }

        MaterialTextView materialTextView = (MaterialTextView) itemView;

        String txt = contacto.isContactoPrincipal() ? "Principais" : "Secundários";

        try {
            materialTextView.setTextFuture(PrecomputedTextCompat.getTextFuture(txt, materialTextView.getTextMetricsParamsCompat(), executor));
        } catch (Exception e) {
            materialTextView.setText(txt);
        }
    }

    public static class ContactViewHolder extends RecyclerView.ViewHolder {
        public ContactViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }

    public static class TypeContactsViewHolder extends RecyclerView.ViewHolder {
        public TypeContactsViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
}