package kmlib.starsector.scripts;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.campaign.SectorAPI;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pins {@link SectorScripts#addIfAbsent}'s contract:
 *  - skips the factory when an instance of the class is already in
 *    the script list,
 *  - calls the factory and {@link SectorAPI#addScript} when none
 *    is present,
 *  - silently no-ops on null sector,
 *  - subclasses count as instances (per Class.isInstance).
 */
final class SectorScriptsTest {

    private interface DemoScript extends EveryFrameScript {}
    private static final class DemoScriptImpl implements DemoScript {
        @Override public boolean isDone() { return false; }
        @Override public boolean runWhilePaused() { return false; }
        @Override public void advance(float amount) {}
    }

    @Test
    void skips_factory_when_a_matching_script_is_already_present() {
        SectorAPI sector = mock(SectorAPI.class);
        List<EveryFrameScript> scripts = new ArrayList<>();
        scripts.add(new DemoScriptImpl());
        when(sector.getScripts()).thenReturn(scripts);

        boolean[] factoryFired = { false };
        SectorScripts.addIfAbsent(sector, DemoScript.class, () -> {
            factoryFired[0] = true;
            return new DemoScriptImpl();
        });

        assertThat(factoryFired[0]).isFalse();
        verify(sector, never()).addScript(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void installs_when_no_matching_script_is_present() {
        SectorAPI sector = mock(SectorAPI.class);
        when(sector.getScripts()).thenReturn(new ArrayList<>());

        DemoScriptImpl created = new DemoScriptImpl();
        SectorScripts.addIfAbsent(sector, DemoScript.class, () -> created);

        verify(sector).addScript(created);
    }

    @Test
    void installs_when_scripts_list_is_null() {
        // Defensive: very-early-load may surface a sector whose
        // scripts list has not been initialised yet.
        SectorAPI sector = mock(SectorAPI.class);
        when(sector.getScripts()).thenReturn(null);

        DemoScriptImpl created = new DemoScriptImpl();
        SectorScripts.addIfAbsent(sector, DemoScript.class, () -> created);

        verify(sector).addScript(created);
    }

    @Test
    void null_sector_is_a_silent_noop() {
        boolean[] factoryFired = { false };
        SectorScripts.addIfAbsent(null, DemoScript.class, () -> {
            factoryFired[0] = true;
            return new DemoScriptImpl();
        });

        assertThat(factoryFired[0]).isFalse();
    }

    @Test
    void instance_check_honours_subclass_assignability() {
        // A script of a subclass already in the list must satisfy
        // an isAbsent check on its parent type - same semantics
        // Sector.removeScriptsOfClass would use.
        class Parent implements EveryFrameScript {
            @Override public boolean isDone() { return false; }
            @Override public boolean runWhilePaused() { return false; }
            @Override public void advance(float amount) {}
        }
        class Child extends Parent {}

        SectorAPI sector = mock(SectorAPI.class);
        List<EveryFrameScript> scripts = new ArrayList<>();
        scripts.add(new Child());
        when(sector.getScripts()).thenReturn(scripts);

        SectorScripts.addIfAbsent(sector, Parent.class, Parent::new);

        verify(sector, never()).addScript(org.mockito.ArgumentMatchers.any());
    }
}
