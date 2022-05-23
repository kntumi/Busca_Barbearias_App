package kev.app.timeless.model;

import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import org.jetbrains.annotations.NotNull;

@Entity(tableName = "Users")
public class User{
    @NotNull
    @PrimaryKey
    @ColumnInfo(name = "id")
    private String id;

    @ColumnInfo(name = "email")
    private String email;

    @Ignore
    public User() {
    }

    public User(@NotNull String id, String email) {
        this.id = id;
        this.email = email;
    }

    @NotNull
    public String getId() {
        return id;
    }

    @Nullable
    public String getEmail() {
        return email;
    }

    public void setId(@NotNull String id) {
        this.id = id;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @Override
    public String toString() {
        return "User{" +
                "id='" + id + '\'' +
                ", email='" + email + '\'' +
                '}';
    }
}