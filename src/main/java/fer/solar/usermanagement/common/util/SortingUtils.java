package fer.solar.usermanagement.common.util;

import java.util.Comparator;
import java.util.function.Function;

import lombok.experimental.UtilityClass;

@UtilityClass
public class SortingUtils {

    public static <T> Comparator<T> createNullsFirstCaseInsensitiveComparator(Function<? super T, String> keyExtractor) {
        Comparator<String> baseComparator = Comparator.nullsFirst(String.CASE_INSENSITIVE_ORDER);
        return Comparator.comparing(keyExtractor, baseComparator);
    }

}