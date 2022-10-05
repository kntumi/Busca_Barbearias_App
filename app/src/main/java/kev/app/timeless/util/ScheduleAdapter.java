package kev.app.timeless.util;

import android.text.TextUtils;
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
    private final View.OnClickListener onClickListener;
    private final LoggedInListener loggedInListener;

    public ScheduleAdapter(@NonNull DiffUtil.ItemCallback<Horário> diffCallback, View.OnClickListener onClickListener, LoggedInListener loggedInListener) {
        super(diffCallback);
        this.onClickListener = onClickListener;
        this.loggedInListener = loggedInListener;
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
        holder.edit.setVisibility(TextUtils.isEmpty(loggedInListener.loggedInUserId()) ? View.GONE : View.VISIBLE);

        String horaAbertura = String.valueOf(horário.getHoraAbertura()), horaEncerramento = String.valueOf(horário.getHoraEncerramento());

        horaAbertura = horaAbertura.length() - horaAbertura.indexOf(".") == 2 ? horaAbertura.concat("0") : horaAbertura;
        horaEncerramento = horaEncerramento.length() - horaEncerramento.indexOf(".") == 2 ? horaEncerramento.concat("0") : horaEncerramento;

        holder.txtHorario.setText(horaAbertura.replace(".", ":").concat(" - ").concat(horaEncerramento.replace(".", ":")));
    }

    @Override
    public void onViewAttachedToWindow(@NonNull ScheduleViewHolder holder) {
        super.onViewAttachedToWindow(holder);
        holder.show.setOnClickListener(onClickListener);
        holder.edit.setOnClickListener(onClickListener);
    }

    @Override
    public void onViewDetachedFromWindow(@NonNull ScheduleViewHolder holder) {
        super.onViewDetachedFromWindow(holder);
        holder.show.setOnClickListener(null);
        holder.edit.setOnClickListener(null);
    }

    public static class ScheduleViewHolder extends RecyclerView.ViewHolder {
        private final TextView txtDiaSemana;
        private final TextView txtHorario;
        private final ImageView show;
        private final ImageView edit;

        public ScheduleViewHolder(@NonNull View itemView) {
            super(itemView);
            this.txtDiaSemana =  itemView.findViewById(R.id.txtDiaSemana);
            this.txtHorario = itemView.findViewById(R.id.txtHorario);
            this.show = itemView.findViewById(R.id.show);
            this.edit = itemView.findViewById(R.id.edit);
        }

        public String getDiaSemana (Horário horário) {
            String diaSemana;

            switch (horário.getDia()) {
                case Calendar.SUNDAY: diaSemana = "Domingo";
                    break;
                case Calendar.MONDAY: diaSemana = "Segunda-Feira";
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