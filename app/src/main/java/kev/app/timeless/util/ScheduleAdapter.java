package kev.app.timeless.util;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Calendar;

import kev.app.timeless.R;
import kev.app.timeless.model.Horário;

public class ScheduleAdapter extends ListAdapter<Horário, ScheduleAdapter.ScheduleViewHolder> {
    private View.OnClickListener onClickListener;

    public ScheduleAdapter(@NonNull DiffUtil.ItemCallback<Horário> diffCallback, View.OnClickListener onClickListener) {
        super(diffCallback);
        this.onClickListener = onClickListener;
    }

    @NonNull
    @Override
    public ScheduleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ScheduleViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_schedule_item, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ScheduleViewHolder holder, int positionViewHolder) {
        Horário horário = getItem(positionViewHolder);

        if (horário == null) {
            return;
        }

        holder.txtDiaSemana.setText(holder.getDiaSemana(horário));

        String horaAbertura = String.valueOf(horário.getHoraAbertura()), horaEncerramento = String.valueOf(horário.getHoraEncerramento());

        horaAbertura = horaAbertura.length() - horaAbertura.indexOf(".") == 2 ? horaAbertura.concat("0") : horaAbertura;
        horaEncerramento = horaEncerramento.length() - horaEncerramento.indexOf(".") == 2 ? horaEncerramento.concat("0") : horaEncerramento;

        holder.txtHorario.setText(horaAbertura.replace(".", ":").concat(" - ").concat(horaEncerramento.replace(".", ":")));
    }

    @Override
    public void onViewAttachedToWindow(@NonNull ScheduleViewHolder holder) {
        super.onViewAttachedToWindow(holder);
        holder.btnShow.setOnClickListener(onClickListener);
    }

    @Override
    public void onViewDetachedFromWindow(@NonNull ScheduleViewHolder holder) {
        super.onViewDetachedFromWindow(holder);
        holder.btnShow.setOnClickListener(null);
    }

    public static class ScheduleViewHolder extends RecyclerView.ViewHolder {
        private TextView txtDiaSemana, txtHorario;
        private ImageView btnShow;

        public ScheduleViewHolder(@NonNull View itemView) {
            super(itemView);
            this.txtDiaSemana =  itemView.findViewById(R.id.txtDiaSemana);
            this.txtHorario = itemView.findViewById(R.id.txtHorario);
            this.btnShow = itemView.findViewById(R.id.btnShow);
        }

        public String getDiaSemana (Horário horário) {
            String diaSemana;

            switch (horário.getDia()) {
                case Calendar.SUNDAY: diaSemana = "Domingo";
                    break;
                case Calendar.MONDAY: diaSemana = "Segunda-feira";
                    break;
                case Calendar.TUESDAY: diaSemana = "Terça-Feira";
                    break;
                case Calendar.WEDNESDAY: diaSemana = "Quarta-Feira";
                    break;
                case Calendar.THURSDAY: diaSemana = "Quinta-Feira";
                    break;
                case Calendar.FRIDAY: diaSemana = "Sexta-Feira";
                    break;
                case Calendar.SATURDAY: diaSemana = "Sábado";
                    break;
                default: diaSemana = "";
            }

            return diaSemana;
        }
    }
}