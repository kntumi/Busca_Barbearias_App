package kev.app.timeless.room;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

import io.reactivex.Completable;
import kev.app.timeless.model.User;

@Dao
public interface UserDao {
    @Query("SELECT * FROM Users")
    LiveData<List<User>> buscarUsuarioActual();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    Completable inserirUsuario(User user);

    @Query("DELETE FROM Users")
    Completable apagarUsuario();
}
