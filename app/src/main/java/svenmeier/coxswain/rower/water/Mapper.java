package svenmeier.coxswain.rower.water;

import java.util.ArrayList;
import java.util.List;

import svenmeier.coxswain.gym.Snapshot;

/**
 + '14A': {'type': 'avg_distance_cmps', 'size': 'double', 'base': 16}
 + '1A9': {'type': 'stroke_rate',       'size': 'single', 'base': 16}
 + '1E1': {'type': 'display_sec',       'size': 'single', 'base': 10}
 + '1E2': {'type': 'display_min',       'size': 'single', 'base': 10}
 + '1E3': {'type': 'display_hr',        'size': 'single', 'base': 10}
 - '1E0': {'type': 'display_sec_dec',   'size': 'single', 'base': 10}
 - '055': {'type': 'total_distance_m',  'size': 'double', 'base': 16}
 - '140': {'type': 'total_strokes',     'size': 'double', 'base': 16}
 - '08A': {'type': 'total_kcal',        'size': 'triple', 'base': 16}
 */
public class Mapper {

    public static final String INIT = "USB";
    public static final String RESET = "RESET";
    public static final String VERSION = "IV?";

    private int cycle = 0;

    private List<Field> fields = new ArrayList<>();

    public Mapper() {
        fields.add(new Field(0x140, Field.DOUBLE, Field.HEX) {
            @Override
            protected void onUpdate(short value, Snapshot memory) {
                memory.strokes = value;
            }
        });

        fields.add(new Field(0x057, Field.DOUBLE, Field.HEX) {
            @Override
            protected void onUpdate(short value, Snapshot memory) {
                memory.distance = value;
            }
        });

        fields.add(new Field(0x14A, Field.DOUBLE, Field.HEX) {
            @Override
            protected void onUpdate(short value, Snapshot memory) {
                memory.speed = value;
            }
        });

        fields.add(new Field(0x1A9, Field.SINGLE, Field.HEX) {
            @Override
            protected void onUpdate(short value, Snapshot memory) {
                memory.strokeRate = value;
            }
        });

        fields.add(new Field(0x1A0, Field.SINGLE, Field.HEX) {
            @Override
            protected void onUpdate(short value, Snapshot memory) {
                memory.pulse = value;
            }
        });
    }

    public Field cycle() {
        Field field = fields.get(cycle);

        cycle = (cycle + 1) % fields.size();

        return field;
    }

    public void map(String message, Snapshot memory) {
        for (Field field : fields) {
            field.update(message, memory);
        }
    }
}
