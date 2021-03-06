package kilanny.muslimalarm.data;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

@Dao
public interface AlarmDao {

    @Query("SELECT * FROM alarm ORDER BY enabled DESC, id")
    Alarm[] getAll();

    @Query("SELECT * FROM alarm WHERE enabled = 1 ORDER BY id")
    Alarm[] getAllEnabled();

    @Query("SELECT * FROM alarm WHERE id = :id")
    Alarm getById(int id);

    @Insert
    long insert(Alarm alarm);

    @Update
    void update(Alarm alarm);

    @Delete
    void delete(Alarm alarm);

    @Query("SELECT COUNT(*) FROM alarm")
    int count();

    @Query("SELECT COUNT(*) FROM alarm WHERE dismiss_alarm_barcode_id = :barcodeId")
    int countByBarcodeId(int barcodeId);
}
