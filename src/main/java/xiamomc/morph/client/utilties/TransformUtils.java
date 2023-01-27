package xiamomc.morph.client.utilties;

import org.apache.commons.lang3.NotImplementedException;
import xiamomc.morph.client.graphics.transforms.easings.Easing;

import java.util.Arrays;

public class TransformUtils
{
    public static <TValue> TValue valueAt(double progress, TValue startVal, TValue endVal, Easing easing)
    {
        return ValueTransformer.create(progress, startVal, endVal, easing);
    }

    private static class ValueTransformer
    {
        public static Double valueAt(double progress, double startVal, double endVal, Easing easing)
        {
            if (startVal == endVal) return endVal;
            if (progress <= 0) return startVal;
            if (progress >= 1) return endVal;

            return startVal + (endVal - startVal) * easing.getImpl().apply(progress);
        }

        public static Float valueAt(double progress, float startVal, float endVal, Easing easing)
        {
            return valueAt(progress, (double) startVal, endVal, easing).floatValue();
        }

        public static Long valueAt(double progress, long startVal, long endVal, Easing easing)
        {
            return Math.round(valueAt(progress, (double) startVal, endVal, easing));
        }

        public static Integer valueAt(double progress, int startVal, int endVal, Easing easing)
        {
            return (int) Math.round(valueAt(progress, (double) startVal, endVal, easing));
        }

        public static Short valueAt(double progress, short startVal, short endVal, Easing easing)
        {
            return (short) Math.round(valueAt(progress, (double) startVal, endVal, easing));
        }

        public static <TValue> TValue create(double progress, TValue startVal, TValue endVal, Easing easing)
        {
            var method = Arrays.stream(ValueTransformer.class.getMethods())
                    .filter(m -> m.getReturnType() == startVal.getClass())
                    .findFirst().orElse(null);

            if (method == null)
                throw new NotImplementedException("No such transform method for type " + startVal.getClass());

            try
            {
                return (TValue) method.invoke(null, progress, startVal, endVal, easing);
            }
            catch (Exception e)
            {
                throw new RuntimeException(e);
            }
        }
    }
}