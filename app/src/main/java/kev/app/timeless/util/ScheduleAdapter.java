package kev.app.timeless.util;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;

import java.util.Calendar;

import kev.app.timeless.R;
import kev.app.timeless.model.Horário;

public class ScheduleAdapter extends ListAdapter<Horário, ScheduleAdapter.ScheduleViewHolder> {
    private CompoundButton.OnCheckedChangeListener onCheckedChangeListener;

    public ScheduleAdapter(@NonNull DiffUtil.ItemCallback<Horário> diffCallback, CompoundButton.OnCheckedChangeListener onCheckedChangeListener) {
        super(diffCallback);
        this.onCheckedChangeListener = onCheckedChangeListener;
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
        holder.chip.setOnCheckedChangeListener(onCheckedChangeListener);
    }

    @Override
    public void onViewDetachedFromWindow(@NonNull ScheduleViewHolder holder) {
        super.onViewDetachedFromWindow(holder);
        holder.chip.setOnCheckedChangeListener(null);
    }

    public static class ScheduleViewHolder extends RecyclerView.ViewHolder {
        private Chip chip;

        public ScheduleViewHolder(@NonNull View itemView) {
            super(itemView);
            this.chip =  itemView.findViewById(R.id.txt);
        }

        public void atualizarViews(Chip txtDiaSemana, Horário horário) {
            switch (horário.getDia()) {
                case Calendar.SUNDAY: txtDiaSemana.setText("Dom");
                    break;
                case Calendar.MONDAY: txtDiaSemana.setText("Seg");
                    break;
                case Calendar.TUESDAY: txtDiaSemana.setText("Ter");
                    break;
                case Calendar.WEDNESDAY: txtDiaSemana.setText("Qua");
                    break;
                case Calendar.THURSDAY: txtDiaSemana.setText("Qui");
                    break;
                case Calendar.FRIDAY: txtDiaSemana.setText("Sex");
                    break;
                case Calendar.SATURDAY: txtDiaSemana.setText("Sáb");
                    break;
            }
        }
    }
}
