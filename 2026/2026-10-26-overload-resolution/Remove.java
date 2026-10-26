import java.util.ArrayList;
import java.util.List;

public class Remove {

    static List<Integer> fresh() {
        return new ArrayList<>(List.of(1, 2, 3));
    }

    public static void main(String[] args) {
        List<Integer> list = new ArrayList<>(List.of(1, 2, 3));

        // Both calls on one list, exactly as in the post: [1, 2, 3] -> [1, 3] -> [3]
        list.remove(1);
        System.out.println("remove(1)                  -> " + list);
        list.remove(Integer.valueOf(1));
        System.out.println("remove(Integer.valueOf(1)) -> " + list);

        List<Integer> cast = fresh();
        cast.remove((Integer) 1);
        System.out.println("remove((Integer) 1)        -> " + cast + "   fresh list");
    }
}
