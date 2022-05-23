package kev.app.timeless.room;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Maybe;
import kev.app.timeless.model.Estabelecimento;

@Dao
public interface EstabelecimentoDao {
    @Query("SELECT * FROM Estabelecimentos WHERE id=:idEstabelecimento")
    Maybe<List<Estabelecimento>> buscarEstabelecimentoPorID(String idEstabelecimento);

    @Query("SELECT * FROM Estabelecimentos WHERE id=:idEstabelecimento")
    LiveData<List<Estabelecimento>> buscarEstabelecimentosPorID(String idEstabelecimento);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    Completable inserirEstabelecimento(Estabelecimento estabelecimento);

    @Query("DELETE FROM Estabelecimentos WHERE id=:idEstabelecimento")
    Completable apagarEstabelecimentoPorID(String idEstabelecimento);
}
