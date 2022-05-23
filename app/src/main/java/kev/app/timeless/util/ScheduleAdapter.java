package kev.app.timeless.util;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;

import java.util.Calendar;

import kev.app.timeless.R;
import kev.app.timeless.model.Horário;

public class ScheduleAdapter extends ListAdapter<Horário, ScheduleAdapter.ScheduleViewHolder> {
    private View.OnClickListener onClickListener;
    private View.OnLongClickListener onLongClickListener;

    public ScheduleAdapter(@NonNull DiffUtil.ItemCallback<Horário> diffCallback, View.OnClickListener onClickListener, View.OnLongClickListener onLongClickListener) {
        super(diffCallback);
        this.onClickListener = onClickListener;
        this.onLongClickListener = onLongClickListener;
    }

    @NonNull
    @Override
    public ScheduleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ScheduleViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_schedule_item, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ScheduleViewHolder holder, int positionViewHolder) {
        holder.atualizarViews(holder.itemView.findViewById(R.id.txt), getItem(positionViewHolder));
    }

    @Override
    public void onViewAttachedToWindow(@NonNull ScheduleViewHolder holder) {
        super.onViewAttachedToWindow(holder);
        holder.itemView.setOnClickListener(onClickListener);
        holder.itemView.setOnLongClickListener(onLongClickListener);
    }

    @Override
    public void onViewDetachedFromWindow(@NonNull ScheduleViewHolder holder) {
        super.onViewDetachedFromWindow(holder);
        holder.itemView.setOnClickListener(null);
        holder.itemView.setOnLongClickListener(null);
    }

    public static class ScheduleViewHolder extends RecyclerView.ViewHolder {
        public ScheduleViewHolder(@NonNull View itemView) {
            super(itemView);
        }

        public void atualizarViews(Chip txtDiaSemana, Horário horário) {
            switch (horário.getDia()) {
                case Calendar.SUNDAY: txtDiaSemana.setText("Domingo");
                    break;
                case Calendar.MONDAY: txtDiaSemana.setText("Segunda");
                    break;
                case Calendar.TUESDAY: txtDiaSemana.setText("Terça");
                    break;
                case Calendar.WEDNESDAY: txtDiaSemana.setText("Quarta");
                    break;
                case Calendar.THURSDAY: txtDiaSemana.setText("Quinta");
                    break;
                case Calendar.FRIDAY: txtDiaSemana.setText("Sexta");
                    break;
                case Calendar.SATURDAY: txtDiaSemana.setText("Sábado");
                    break;
            }
        }
    }
}
