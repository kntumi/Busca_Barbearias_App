package kev.app.timeless.room;

import androidx.room.Database;
import androidx.room.RoomDatabase;

import kev.app.timeless.model.Estabelecimento;
import kev.app.timeless.model.User;

@Database(entities = {User.class}, version = 1, exportSchema = false)
public abstract class UserDatabase extends RoomDatabase {
    public abstract UserDao userDao();
}
