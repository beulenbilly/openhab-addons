package org.openhab.binding.astro.internal.job;

import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.openhab.binding.astro.internal.config.AstroChannelConfig;
import org.openhab.binding.astro.internal.handler.AstroThingHandler;
import org.openhab.binding.astro.internal.model.Range;
import org.openhab.binding.astro.internal.util.DateTimeUtils;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.Thing;

class JobTest {

    @ParameterizedTest
    @MethodSource
    public void testScheduleRange(String thingUID, AstroThingHandler astroHandler, Range range, String channelId,
            int times, Calendar... cals) {

        try (MockedStatic<DateTimeUtils> utils = Mockito.mockStatic(DateTimeUtils.class)) {
            utils.when(() -> DateTimeUtils.isSameDay(ArgumentMatchers.any(Calendar.class),
                    ArgumentMatchers.any(Calendar.class))).thenReturn(true);
            utils.when(() -> DateTimeUtils.isTimeGreaterEquals(ArgumentMatchers.any(Calendar.class),
                    ArgumentMatchers.any(Calendar.class))).thenReturn(true);

            Job.scheduleRange(thingUID, astroHandler, range, channelId);
        }
        ArgumentCaptor<Calendar> calCaptor = ArgumentCaptor.forClass(Calendar.class);
        Mockito.verify(astroHandler, Mockito.times(times)).schedule(ArgumentMatchers.any(Job.class),
                calCaptor.capture());
        for (int i = 0; i < times; i++) {
            Assertions.assertEquals(cals[i], calCaptor.getAllValues().get(i));
        }
    }

    public static Stream<Arguments> testScheduleRange() {
        return Stream.of(
                Arguments.arguments("astro:sun:junit", astroThingHandler("1", astroChannelConfig(0, null, null)),
                        range(null, null), "1", 0, new Calendar[0]),
                Arguments.arguments("astro:sun:junit", astroThingHandler("1", astroChannelConfig(0, null, null)),
                        range(cal(20, 30), cal(21, 30)), "1", 0, new Calendar[] { cal(20, 30), cal(21, 30) }),
                Arguments.arguments("astro:sun:junit", astroThingHandler("1", astroChannelConfig(0, null, "23:30")),
                        range(cal(20, 30), cal(21, 30)), "1", 0, new Calendar[] { cal(20, 30), cal(21, 30) }),
                Arguments.arguments("astro:sun:junit", astroThingHandler("1", astroChannelConfig(0, null, "23:30")),
                        range(null, null), "1", 0, new Calendar[] { cal(23, 30), cal(23, 30) }));
    }

    private static AstroThingHandler astroThingHandler(String channel, AstroChannelConfig config) {
        AstroThingHandler handler = Mockito.mock(AstroThingHandler.class);
        Mockito.when(handler.getThing()).thenReturn(Mockito.mock(Thing.class));
        Mockito.when(handler.getThing().getChannel(channel)).thenReturn(Mockito.mock(Channel.class));
        Mockito.when(handler.getThing().getChannel(channel).getConfiguration())
                .thenReturn(Mockito.mock(Configuration.class));
        Mockito.when(handler.getThing().getChannel(channel).getConfiguration().as(AstroChannelConfig.class))
                .thenReturn(config);
        return handler;
    }

    private static AstroChannelConfig astroChannelConfig(int offset, String earliest, String latest) {
        AstroChannelConfig config = new AstroChannelConfig();
        config.offset = offset;
        config.earliest = earliest;
        config.latest = latest;
        return config;
    }

    private static Range range(Calendar start, Calendar end) {
        return new Range(start, end);
    }

    private static Calendar cal(int hour, int minute) {
        ZonedDateTime zdt = ZonedDateTime.now();
        zdt.with(LocalTime.of(hour, minute, 0, 0));
        return GregorianCalendar.from(zdt);
    }

}
