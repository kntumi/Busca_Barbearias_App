package kev.app.timeless.util;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Calendar;

import kev.app.timeless.R;
import kev.app.timeless.model.Horário;

public class ScheduleListAdapter extends ListAdapter<Horário, ScheduleListAdapter.ScheduleViewHolder> {
    private CompoundButton.OnCheckedChangeListener onCheckedChangeListener;
    private View.OnLongClickListener onLongClickListener;

    public ScheduleListAdapter(@NonNull DiffUtil.ItemCallback<Horário> diffCallback, CompoundButton.OnCheckedChangeListener onCheckedChangeListener, View.OnLongClickListener onLongClickListener) {
        super(diffCallback);
        this.onCheckedChangeListener = onCheckedChangeListener;
        this.onLongClickListener = onLongClickListener;
    }

    @NonNull
    @Override
    public ScheduleListAdapter.ScheduleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ScheduleListAdapter.ScheduleViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_schedule_item, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ScheduleListAdapter.ScheduleViewHolder holder, int positionViewHolder) {
        holder.atualizarViews(holder.itemView.findViewById(R.id.txt), getItem(positionViewHolder));
    }

    @Override
    public void onViewAttachedToWindow(@NonNull ScheduleListAdapter.ScheduleViewHolder holder) {
        super.onViewAttachedToWindow(holder);
        holder.itemView.setOnLongClickListener(onLongClickListener);
    }

    @Override
    public void onViewDetachedFromWindow(@NonNull ScheduleListAdapter.ScheduleViewHolder holder) {
        super.onViewDetachedFromWindow(holder);
        holder.itemView.setOnLongClickListener(null);
    }

    public static class ScheduleViewHolder extends RecyclerView.ViewHolder {
        public ScheduleViewHolder(@NonNull View itemView) {
            super(itemView);
        }

        public void atualizarViews(TextView txtDiaSemana, Horário horário) {
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
