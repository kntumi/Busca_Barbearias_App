package kev.app.timeless.room;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

import io.reactivex.Completable;
import kev.app.timeless.model.Localizacao;

@Dao
public interface LocalizacaoDao {
    @Query("SELECT * FROM Localizacoes")
    LiveData<List<Localizacao>> buscarLocalizacaoActual();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    Completable inserirLocalizacao(Localizacao localizacao);
}
