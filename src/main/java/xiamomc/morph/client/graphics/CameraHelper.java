package xiamomc.morph.client.graphics;

import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.BlockView;
import net.minecraft.world.RaycastContext;
import xiamomc.morph.client.DisguiseSyncer;
import xiamomc.morph.client.MorphClient;
import xiamomc.pluginbase.Bindables.Bindable;

public class CameraHelper
{
    public static Bindable<Boolean> isThirdPerson = new Bindable<>(false);

    public float onEyeHeightCall(Entity instance, BlockView area)
    {
        if (instance == null) return 0f;

        var current = DisguiseSyncer.currentEntity.get();
        var client = MorphClient.getInstance();

        if (current != null && client.morphManager.selfVisibleEnabled.get() && client.getModConfigData().changeCameraHeight)
        {
            if (current.getStandingEyeHeight() <= instance.getStandingEyeHeight())
            {
                var vehicle = instance.getVehicle();

                if (vehicle != null)
                    return Math.max(current.getStandingEyeHeight(), vehicle.getStandingEyeHeight() + 0.15f);

                return current.getStandingEyeHeight();
            }

            var pos = instance.getEyePos();
            var targetPos = pos.add(0, current.getStandingEyeHeight() - instance.getStandingEyeHeight(), 0);

            var rayCastContext = new RaycastContext(pos, targetPos,
                    RaycastContext.ShapeType.VISUAL, RaycastContext.FluidHandling.NONE, instance);

            var rayCast = area.raycast(rayCastContext);

            double distance = current.getStandingEyeHeight();
            if (rayCast.getType() == HitResult.Type.BLOCK)
            {
                distance = instance.getStandingEyeHeight();
                //distance = instance.getStandingEyeHeight() + rayCast.getPos().distanceTo(pos) - 0.375f;
            }

            //LoggerFactory.getLogger("dddd").info("type? " + (rayCast.getType()) + " :: distance? " + distance);

            return (float) distance;
        }

        return instance.getStandingEyeHeight();
    }

    public void onCameraUpdate(Camera camera)
    {
    }
}
