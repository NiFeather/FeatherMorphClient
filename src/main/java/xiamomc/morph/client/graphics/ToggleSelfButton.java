package xiamomc.morph.client.graphics;

import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import xiamomc.morph.client.MorphClient;
import xiamomc.pluginbase.Bindables.Bindable;
import xiamomc.morph.client.screens.disguise.DisguiseScreen;

import java.util.List;

public class ToggleSelfButton extends ButtonWidget
{
    private static Text getSwitchTextFrom(boolean val)
    {
        return Text.literal(val ? "I" : "O")
                .setStyle(Style.EMPTY.withColor(val ? TextColor.parse("green") : TextColor.parse("red")));
    }

    public ToggleSelfButton(int x, int y, int width, int height, boolean toggled, DisguiseScreen screen)
    {
        super(x, y, width, height, getSwitchTextFrom(toggled), (button) -> {}, ButtonWidget.DEFAULT_NARRATION_SUPPLIER);
        this.toggleBindable.set(toggled);
        this.toggleBindable.bindTo(MorphClient.getInstance().morphManager.selfVisibleEnabled);
        this.toggleBindable.onValueChanged((o, n) ->
        {
            this.setMessage(getSwitchTextFrom(n));
        }, true);

        this.screen = screen;
    }

    private final DisguiseScreen screen;

    private final Bindable<Boolean> toggleBindable = new Bindable<>();

    public Bindable<Boolean> getBindable()
    {
        return toggleBindable;
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta)
    {
        super.render(matrices, mouseX, mouseY, delta);

        if (screen != null && this.isHovered())
            screen.renderOrderedTooltip(matrices, tooltips, mouseX, mouseY);
    }

    private final List<OrderedText> tooltips = List.of(Text.translatable("key.morphclient.toggle").asOrderedText());

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button)
    {
        var success = super.mouseClicked(mouseX, mouseY, button);

        if (success)
        {
            var val = !this.toggleBindable.get();
            this.toggleBindable.set(val);

            var modInstance = MorphClient.getInstance();
            var config = modInstance.getModConfigData();

            modInstance.updateClientView(config.allowClientView, val);
        }

        return success;
    }
}
