package kev.app.timeless.room;

import androidx.room.Database;
import androidx.room.RoomDatabase;

import kev.app.timeless.model.Localizacao;

@Database(entities = {Localizacao.class}, version = 1, exportSchema = false)
public abstract class LocalizacaoDatabase extends RoomDatabase {
    public abstract LocalizacaoDao localizacaoDao();
}
