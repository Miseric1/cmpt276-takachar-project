package com.example.demo;

import com.example.demo.dto.knowledge.KnowledgeRequest;
import com.example.demo.dto.knowledge.KnowledgeSummary;
import com.example.demo.model.PublicationStatus;
import com.example.demo.model.DiagnosticNode;
import com.example.demo.model.DiagnosticOption;
import com.example.demo.model.KnowledgeArticle;
import com.example.demo.model.User;
import com.example.demo.repository.DiagnosticNodeRepository;
import com.example.demo.repository.KnowledgeArticleRepository;
import com.example.demo.service.KnowledgeService;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.PageRequest;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Locale;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * {@code seedFaqArticles} below is gated by the {@code app.seed-demo-data}
 * property (see {@code @ConditionalOnProperty}) rather than a Spring profile,
 * because no test in this project activates a "test" profile — a
 * {@code @Profile("!test")} guard would silently never trigger. Setting
 * {@code app.seed-demo-data=false} in src/test/resources/application.properties
 * disables it for every test in the suite, with no changes needed to
 * individual test classes. Without this, the 18 seeded articles leak into
 * whatever database a test's Spring context uses, making keyword/category
 * search assertions (e.g. "first result" or "duplicate title") match
 * unexpected extra articles.
 *
 * {@code createAdmin} is intentionally left unguarded — there's no evidence
 * it causes test failures, and disabling it risks silently breaking some
 * other test that depends on the seeded admin user existing.
 */

@Configuration
public class DataInitializer {

    @Bean
    @Order(1)
    public CommandLineRunner createAdmin(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            if (userRepository.findByEmail("admin@test.com").isEmpty()) {
                User admin = new User();
                admin.setEmail("admin@test.com");
                admin.setPassword(passwordEncoder.encode("password123"));
                admin.setRole("ADMIN");
                userRepository.save(admin);
                System.out.println("Admin user created: admin@test.com / password123");
            }
        };
    }

    /**
     * Seeds placeholder FAQ / Knowledge Base articles that don't already exist,
     * so the FAQ page has display-ready content for this iteration. Checked by
     * title rather than "is the table empty," so a manually-created test
     * article won't cause the rest of the placeholders to be skipped. These are
     * created through the same KnowledgeService the REST API uses, so they
     * behave as ordinary articles afterward — an admin can edit or delete any
     * of them from the FAQ page exactly like one they created themselves.
     */
    @Bean
    @Order(2)
    @ConditionalOnProperty(name = "app.seed-demo-data", havingValue = "true", matchIfMissing = true)
    public CommandLineRunner seedFaqArticles(KnowledgeService knowledgeService) {
        return args -> {
            // Check per-article rather than "does anything exist" — otherwise a
            // single manually-created test article (from trying out Add Article)
            // would cause every placeholder below to be skipped, not just that one.
            Set<String> existingTitles = knowledgeService
                    .search(null, null, null, null, null, PageRequest.of(0, 500))
                    .getContent()
                    .stream()
                    .map(KnowledgeSummary::title)
                    .map(title -> title.toLowerCase(Locale.ROOT))
                    .collect(Collectors.toSet());

            int created = 0;

            for (String[] entry : PLACEHOLDER_ARTICLES) {
                String title = entry[0];
                String category = entry[1];
                String body = entry[2];

                if (existingTitles.contains(title.toLowerCase(Locale.ROOT))) {
                    continue;
                }

                KnowledgeRequest request = new KnowledgeRequest(
                        title,
                        null,
                        body,
                        category,
                        Set.of(),
                        Set.of(),
                        "admin@test.com",
                        PublicationStatus.PUBLISHED);

                knowledgeService.create(request, "system");
                created++;
            }

            if (created > 0) {
                System.out.println("Seeded " + created + " placeholder FAQ articles.");
            }
        };
    }

    @Bean
    @Order(3)
    @ConditionalOnProperty(name = "app.seed-diagnostic-data", havingValue = "true", matchIfMissing = true)
    public CommandLineRunner seedDiagnosticTree(DiagnosticNodeRepository nodeRepository,
                                                KnowledgeArticleRepository articleRepository) {
        return args -> {
            if (nodeRepository.count() > 0) return;

            // ── Category A: Device is not powering on ───────────────────────
            DiagnosticNode lA1 = resolutionNode("cat-a-l1",
                    "Fault L1 \u2014 Missing or blown fuse. Insert 200mA fuses into both fuse holders "
                            + "(FH1 and FH2) on the PCB. If fuses were already present, they may have blown "
                            + "due to a short circuit or overcurrent. Replace them and inspect the board for "
                            + "any conductive material before powering on again.", article(articleRepository, "Fault L1: Missing or blown fuse"));
            DiagnosticNode lA2 = resolutionNode("cat-a-l2",
                    "Fault L2 \u2014 12V supply not connected. Connect a stable 12V DC power supply to the "
                            + "12V power connector on the PCB. Ensure the polarity is correct and the supply "
                            + "provides sufficient current for the board.", article(articleRepository, "Fault L2: 12V supply not connected"));
            DiagnosticNode lA3 = resolutionNode("cat-a-l3",
                    "Fault L3 \u2014 Power circuit failure, likely buck converter or LDO issue. The 12V to "
                            + "4.7-4.8V power conversion stage may have failed. Check the TPS54202 buck "
                            + "converter output using a multimeter at the test points. If the output is not "
                            + "within 4.7-4.8V, the power circuit requires inspection or PCB repair. Do not "
                            + "keep operating the board in this state.", article(articleRepository, "Fault L3: Power circuit failure, likely buck converter or LDO issue"));
            DiagnosticNode lA4 = resolutionNode("cat-a-l4",
                    "Fault L4 \u2014 LDO not producing 3.3V, ESP32-S3 not receiving power. The regulator "
                            + "stepping 4.7-4.8V down to 3.3V for the ESP32-S3 may have failed. Measure the "
                            + "LDO output with a multimeter. If the output is not 3.3V, replace the LDO or "
                            + "open a support ticket with your board serial number.", article(articleRepository, "Fault L4: LDO not producing 3.3V, ESP32-S3 not receiving power"));
            DiagnosticNode lA5 = resolutionNode("cat-a-l5",
                    "Fault L5 \u2014 Board is powered but firmware may not be running. Power is reaching the "
                            + "ESP32-S3 correctly, so the issue may be with the firmware. Check if LED D11 "
                            + "(Heartbeat Indicator) is blinking. If not, the firmware may have crashed or is "
                            + "not flashed. Press S2 (Enable Switch) to reset the microprocessor. If the issue "
                            + "persists, reflash the firmware through the USB port.", article(articleRepository, "Fault L5: Board is powered but firmware may not be running"));

            DiagnosticNode qA5 = questionNode("cat-a-q5",
                    "Is LED D2 (ESP32-S3 power indicator) lit?", false, List.of(
                            treeOption("No, D2 is off", lA4.getId(), 0),
                            treeOption("Yes, D2 is lit", lA5.getId(), 1)));
            DiagnosticNode qA4 = questionNode("cat-a-q4",
                    "Is LED D6 (Power circuit voltage indicator) lit?", false, List.of(
                            treeOption("No, D6 is off", lA3.getId(), 0),
                            treeOption("Yes, D6 is lit", qA5.getId(), 1)));
            DiagnosticNode qA3 = questionNode("cat-a-q3",
                    "Is the 12V DC power supply connected to the power connector on the board?", false, List.of(
                            treeOption("No", lA2.getId(), 0),
                            treeOption("Yes, 12V is connected", qA4.getId(), 1)));
            DiagnosticNode qA2 = questionNode("cat-a-q2",
                    "Are both fuse holders (FH1 and FH2) properly inserted with 200mA fuses?", false, List.of(
                            treeOption("No / Not sure", lA1.getId(), 0),
                            treeOption("Yes, fuses are in place", qA3.getId(), 1)));

            // ── Category B: Modbus / HMI communication issue ────────────────
            DiagnosticNode lB6 = resolutionNode("cat-b-l6",
                    "Fault L6 \u2014 RS-485 bus not connected. Securely connect the RS-485 cable to the bus "
                            + "connector on the PCB. Make sure the correct terminals (A, B, GND) are connected "
                            + "according to the wiring diagram. A loose or reversed connection will stop "
                            + "Modbus communication completely.", article(articleRepository, "Fault L6: RS-485 bus not connected"));
            DiagnosticNode lB7 = resolutionNode("cat-b-l7",
                    "Fault L7 \u2014 Modbus communication not initiating, possible firmware or transceiver "
                            + "fault. The RS-485 transceiver (MAX22025FAWA+) may be damaged, or the firmware "
                            + "Modbus task is not running. Confirm that D11 (Heartbeat LED) is blinking. If "
                            + "the heartbeat is fine but D4 and D5 stay off, the transceiver IC likely needs "
                            + "replacement. Open a support ticket with your board serial number and setup "
                            + "details.", article(articleRepository, "Fault L7: Modbus communication not initiating, possible firmware or transceiver fault"));
            DiagnosticNode lB8 = resolutionNode("cat-b-l8",
                    "Fault L8 \u2014 HMI not responding to Modbus requests. The ESP32-S3 is sending Modbus "
                            + "requests (Tx active) but receiving no response from the HMI (Rx silent). Check "
                            + "HMI Modbus settings: baud rate, device address, and parity must match the "
                            + "controller setup. Also verify RS-485 wiring polarity, as swapped A and B lines "
                            + "often cause one-way communication.", article(articleRepository, "Fault L8: HMI not responding to Modbus requests"));
            DiagnosticNode lB9 = resolutionNode("cat-b-l9",
                    "Fault L9 \u2014 Modbus communication active but data mismatch. Communication is "
                            + "established, but data values are incorrect. This usually happens when register "
                            + "addresses do not match between the ESP32-S3 firmware and the HMI configuration. "
                            + "Verify that the Modbus register map in the firmware matches the HMI register "
                            + "layout exactly. Cross-check with the provided HMI documentation.", article(articleRepository, "Fault L9: Modbus communication active but data mismatch"));

            DiagnosticNode qB7 = questionNode("cat-b-q7",
                    "Is the RS-485 bus cable securely connected to the RS-485 connector on the board?", false,
                    List.of(
                            treeOption("No / Loose connection", lB6.getId(), 0),
                            treeOption("Yes, cable is connected", lB7.getId(), 1)));
            DiagnosticNode qB6 = questionNode("cat-b-q6",
                    "Are LEDs D4 (Rx) and D5 (Tx) showing any activity?", false, List.of(
                            treeOption("Both are completely off", qB7.getId(), 0),
                            treeOption("Only Tx (D5) is blinking, no Rx (D4)", lB8.getId(), 1),
                            treeOption("Both are blinking but data seems incorrect", lB9.getId(), 2)));

            // ── Category C: Azure / Wi-Fi / Telemetry issue ─────────────────
            DiagnosticNode lC10 = resolutionNode("cat-c-l10",
                    "Fault L10 \u2014 Wi-Fi antenna not connected. Connect the Wi-Fi antenna to the ESP "
                            + "antenna port on the board. Without the antenna, signal strength is extremely "
                            + "poor and the board will likely fail to connect. Wait 30 to 60 seconds after "
                            + "connecting for the device to reconnect.", article(articleRepository, "Fault L10: Wi-Fi antenna not connected"));
            DiagnosticNode lC11 = resolutionNode("cat-c-l11",
                    "Fault L11 \u2014 Wi-Fi not connected, possible credential or signal issue. The board "
                            + "cannot connect to Wi-Fi. Check if Wi-Fi credentials in the firmware are "
                            + "incorrect and reprovision through the SoftAP web portal. Ensure the router is "
                            + "within range and the signal is strong enough. Update credentials if the router "
                            + "SSID or password changed.", article(articleRepository, "Fault L11: Wi-Fi not connected, possible credential or signal issue"));
            DiagnosticNode lC12 = resolutionNode("cat-c-l12",
                    "Fault L12 \u2014 Azure telemetry connection dropped. The device was connected to Azure "
                            + "IoT Hub but dropped off. This is commonly caused by weak Wi-Fi signal (RSSI "
                            + "worse than -75 dBm), a TCP half-open state, or an expired IoT Hub SAS token. "
                            + "The firmware will attempt automatic reconnection. If it does not reconnect "
                            + "after 5 minutes, press S2 (Enable) to restart the firmware.", article(articleRepository, "Fault L12: Azure telemetry connection dropped"));
            DiagnosticNode lC13 = resolutionNode("cat-c-l13",
                    "Fault L13 \u2014 Telemetry sending but not appearing on Azure IoT Hub. The device shows "
                            + "as connected, but data does not show up on the cloud dashboard. Verify that "
                            + "the correct IoT device ID is configured in the firmware against the Azure IoT "
                            + "Hub registry. Check USB serial logs for any JSON payload errors. Contact your "
                            + "Azure administrator to check routing or consumer group setups.", article(articleRepository, "Fault L13: Telemetry sending but not appearing on Azure IoT Hub"));

            DiagnosticNode qC9 = questionNode("cat-c-q9",
                    "Is the Wi-Fi antenna connected to the ESP antenna port?", false, List.of(
                            treeOption("No", lC10.getId(), 0),
                            treeOption("Yes, antenna is connected", lC11.getId(), 1)));
            DiagnosticNode qC8 = questionNode("cat-c-q8",
                    "Is LED D7 (Azure Telemetry indicator) lit or blinking?", false, List.of(
                            treeOption("No, D7 is completely off", qC9.getId(), 0),
                            treeOption("D7 was blinking but has stopped", lC12.getId(), 1),
                            treeOption("D7 is lit but data is not appearing on the Azure dashboard",
                                    lC13.getId(), 2)));

            // ── Category D: SD card / data logging issue ────────────────────
            DiagnosticNode lD14 = resolutionNode("cat-d-l14",
                    "Fault L14 \u2014 SD card not inserted. Insert a compatible SD card (FAT32 formatted, up "
                            + "to 32GB recommended) into the SD card port until it clicks securely. LED D12 "
                            + "(SD Card Write indicator) should start blinking once logging begins.", article(articleRepository, "Fault L14: SD card not inserted"));
            DiagnosticNode lD15 = resolutionNode("cat-d-l15",
                    "Fault L15 \u2014 SD card not being written to, possible format or mount failure. "
                            + "Reformat the SD card as FAT32 using a PC and reinsert it. If it still fails, "
                            + "try a different card to rule out corruption. Inspect the SD card module on the "
                            + "PCB for physical connection issues.", article(articleRepository, "Fault L15: SD card not being written to, possible format or mount failure"));
            DiagnosticNode lD16 = resolutionNode("cat-d-l16",
                    "Fault L16 \u2014 SD card write corruption. Files can corrupt if the board loses power "
                            + "mid-write or if the card is failing. Always power off the board before "
                            + "removing the SD card. Use a high-quality Class 10 card. If issues persist, the "
                            + "SD card module on the PCB may need inspection.", article(articleRepository, "Fault L16: SD card write corruption"));

            DiagnosticNode qD11 = questionNode("cat-d-q11",
                    "Is LED D12 (SD Card write indicator) blinking?", false, List.of(
                            treeOption("No, D12 is off", lD15.getId(), 0),
                            treeOption("Yes, D12 blinks but files look corrupted", lD16.getId(), 1)));
            DiagnosticNode qD10 = questionNode("cat-d-q10",
                    "Is the SD card properly inserted into the SD card port?", false, List.of(
                            treeOption("No", lD14.getId(), 0),
                            treeOption("Yes, SD card is inserted", qD11.getId(), 1)));

            // ── Category E: PWM / Blower control issue ──────────────────────
            DiagnosticNode lE17 = resolutionNode("cat-e-l17",
                    "Fault L17 \u2014 Isolated 5V supply missing, PWM output will not work. The PWM output "
                            + "circuit uses an isolated 5V reference to shift levels from the 3.3V logic on "
                            + "the ESP32-S3. Without this 5V supply, the PWM connector produces no signal. "
                            + "Connect the 5V isolated power supply to the designated connector on the left "
                            + "side of the enclosure.", article(articleRepository, "Fault L17: Isolated 5V supply missing, PWM output will not work"));
            DiagnosticNode lE18 = resolutionNode("cat-e-l18",
                    "Fault L18 \u2014 PWM output connector not wired to blowers. Connect the blower control "
                            + "cables to the Output PWM Connector on the PCB (accessible from the left side of "
                            + "the enclosure). Make sure each blower is connected to its assigned channel per "
                            + "the wiring diagram.", article(articleRepository, "Fault L18: PWM output connector not wired to blowers"));
            DiagnosticNode lE19 = resolutionNode("cat-e-l19",
                    "Fault L19 \u2014 PWM signal present but blowers not responding. Verify that PWM duty "
                            + "cycle settings in the firmware match what the blower driver expects. Check the "
                            + "blower driver or VFD for fault codes. Test the PWM output voltage with a "
                            + "multimeter or oscilloscope at the connector to confirm a 5V referenced signal. "
                            + "If the signal is present, the issue is with the blower or driver rather than "
                            + "the PCB.", article(articleRepository, "Fault L19: PWM signal present but blowers not responding"));

            DiagnosticNode qE13 = questionNode("cat-e-q13",
                    "Are the blower PWM cables connected to the Output PWM Connector?", false, List.of(
                            treeOption("No", lE18.getId(), 0),
                            treeOption("Yes, connected but blowers not responding", lE19.getId(), 1)));
            DiagnosticNode qE12 = questionNode("cat-e-q12",
                    "Is the 5V isolated power supply connected to the isolated power supply connector?", false,
                    List.of(
                            treeOption("No", lE17.getId(), 0),
                            treeOption("Yes, 5V isolated supply is connected", qE13.getId(), 1)));

            // ── Category F: GPS data not showing on the dashboard ───────────
            DiagnosticNode lF20 = resolutionNode("cat-f-l20",
                    "Fault L20 \u2014 Dashboard not receiving any data because Wi-Fi is down. The dashboard "
                            + "cannot receive location data without an active Wi-Fi connection. Resolve Wi-Fi "
                            + "first: check the Wi-Fi antenna connection, confirm credentials via the SoftAP "
                            + "portal, and make sure the router is in range.", article(articleRepository, "Fault L20: Dashboard not receiving any data because Wi-Fi is down"));
            DiagnosticNode lF21 = resolutionNode("cat-f-l21",
                    "Fault L21 \u2014 GPS antenna not connected to the board. Attach the GPS antenna to the "
                            + "GPS port on the PCB. The Neo-M8N GPS module cannot receive satellite signals "
                            + "without an antenna. Make sure the connector is seated firmly.", article(articleRepository, "Fault L21: GPS antenna not connected to the board"));
            DiagnosticNode lF22 = resolutionNode("cat-f-l22",
                    "Fault L22 \u2014 GPS antenna has no clear view of the sky. Place the GPS antenna outdoors "
                            + "or in an unobstructed location to receive satellite signals. Route the cable so "
                            + "the antenna sits open to the sky. Wait 1 to 3 minutes for satellite fix "
                            + "acquisition.", article(articleRepository, "Fault L22: GPS antenna has no clear view of the sky"));
            DiagnosticNode lF23 = resolutionNode("cat-f-l23",
                    "Fault L23 \u2014 GPS module still acquiring satellite fix. The Neo-M8N module needs 1 to "
                            + "3 minutes to lock onto satellites after power-on, especially during a cold "
                            + "start. Wait at least 3 minutes before expecting data on the dashboard.", article(articleRepository, "Fault L23: GPS module still acquiring satellite fix"));
            DiagnosticNode lF24 = resolutionNode("cat-f-l24",
                    "Fault L24 \u2014 GPS module not producing data. Confirm 3.3V power is reaching the GPS "
                            + "module. Check the UART communication between the GPS module and ESP32-S3 via "
                            + "the USB serial monitor to look for NMEA sentences. If no NMEA data appears, the "
                            + "GPS module may be damaged and requires a support ticket.", article(articleRepository, "Fault L24: GPS module not producing data"));

            DiagnosticNode qF17 = questionNode("cat-f-q17",
                    "Has the board been powered on for at least 2 to 3 minutes since placing the antenna "
                            + "outside?", false, List.of(
                            treeOption("No, just powered on", lF23.getId(), 0),
                            treeOption("Yes, waited over 3 minutes, still no data", lF24.getId(), 1)));
            DiagnosticNode qF16 = questionNode("cat-f-q16",
                    "Is the GPS antenna placed outdoors or near an open window with a clear view of the sky?",
                    false, List.of(
                            treeOption("No, antenna is indoors or obstructed", lF22.getId(), 0),
                            treeOption("Yes, antenna is outdoors with clear sky view", qF17.getId(), 1)));
            DiagnosticNode qF15 = questionNode("cat-f-q15",
                    "Is the GPS antenna connected to the GPS antenna port on the board?", false, List.of(
                            treeOption("No / Not sure", lF21.getId(), 0),
                            treeOption("Yes, GPS antenna is connected", qF16.getId(), 1)));
            DiagnosticNode qF14 = questionNode("cat-f-q14",
                    "Is the Wi-Fi connection working on the board? (LED D7 should be active)", false, List.of(
                            treeOption("No, Wi-Fi is not connected", lF20.getId(), 0),
                            treeOption("Yes, Wi-Fi is connected and D7 is active", qF15.getId(), 1)));

            // ── Category G: Drive error code "Er C90" (encoder feedback) ────
            DiagnosticNode lG25 = resolutionNode("cat-g-l25",
                    "Fault L25 \u2014 Loose or disconnected encoder cable connectors. Check the connectors on "
                            + "both ends (drive side and motor side). Ensure both sides are securely inserted "
                            + "and locked in place properly.", article(articleRepository, "Fault L25: Loose or disconnected encoder cable connectors"));
            DiagnosticNode lG26 = resolutionNode("cat-g-l26",
                    "Fault L26 \u2014 Damaged encoder cable (e.g., exposed to hot char in the field). Replace "
                            + "the damaged encoder cable. Preventive action: route all cables inside "
                            + "protective cable ducts and inspect them routinely to protect against field char "
                            + "damage.", article(articleRepository, "Fault L26: Damaged encoder cable"));
            DiagnosticNode lG27 = resolutionNode("cat-g-l27",
                    "Fault L27 \u2014 Damaged connector pins on the motor encoder socket. Carefully straighten "
                            + "bent pins or replace the damaged encoder connector assembly on the motor.", article(articleRepository, "Fault L27: Damaged connector pins on the motor encoder socket"));
            DiagnosticNode lG28 = resolutionNode("cat-g-l28",
                    "Fault L28 \u2014 Internal encoder failure on the motor. Replace the faulty motor or "
                            + "replace its internal encoder unit.", article(articleRepository, "Fault L28: Internal encoder failure on the motor"));
            DiagnosticNode lG29 = resolutionNode("cat-g-l29",
                    "Fault L29 \u2014 Drive parameter corruption or internal drive controller fault. Perform a "
                            + "factory reset on the drive parameters and re-configure all parameter settings "
                            + "according to the drive manual SOP.", article(articleRepository, "Fault L29: Drive parameter corruption or internal drive controller fault"));

            DiagnosticNode qG21 = questionNode("cat-g-q21",
                    "Have you tested the drive feedback loop using a known-good spare motor?", false, List.of(
                            treeOption("Spare motor works correctly", lG28.getId(), 0),
                            treeOption("Error persists / No spare motor available", lG29.getId(), 1)));
            DiagnosticNode qG20 = questionNode("cat-g-q20",
                    "Are any pins inside the motor's encoder connector socket bent or broken?", false, List.of(
                            treeOption("Yes, pins are bent or broken", lG27.getId(), 0),
                            treeOption("No, pins are in good condition", qG21.getId(), 1)));
            DiagnosticNode qG19 = questionNode("cat-g-q19",
                    "Is there any visible physical damage, cuts, or burn marks on the encoder cable?", false,
                    List.of(
                            treeOption("Yes, cable shows cuts or heat damage", lG26.getId(), 0),
                            treeOption("No, cable appears visually intact", qG20.getId(), 1)));
            DiagnosticNode qG18 = questionNode("cat-g-q18",
                    "Have you checked the physical connections of the encoder cable on both the drive end and "
                            + "motor end?", false, List.of(
                            treeOption("No / Loose connection", lG25.getId(), 0),
                            treeOption("Yes, both ends are securely connected", qG19.getId(), 1)));

            // ── Category H: System boot failure & auger inactivity ──────────
            DiagnosticNode lH30 = resolutionNode("cat-h-l30",
                    "Fault L30 \u2014 Damaged VPR or out-of-spec incoming power supply. Check incoming PDB "
                            + "voltage. If incoming line voltage is within range (200-220V, 50 Hz) but VPR "
                            + "displays nothing or stays tripped, the VPR unit is damaged and requires "
                            + "replacement.", article(articleRepository, "Fault L30: Damaged VPR or out-of-spec incoming power supply"));
            DiagnosticNode lH31 = resolutionNode("cat-h-l31",
                    "Fault L31 \u2014 Faulty main contactor or control loop fault. Verify control voltage "
                            + "across the contactor coil. If control voltage is present but the contactor "
                            + "fails to pull in and hold, replace the faulty contactor.", article(articleRepository, "Fault L31: Faulty main contactor or control loop fault"));
            DiagnosticNode lH32 = resolutionNode("cat-h-l32",
                    "Fault L32 \u2014 Wiring from contactor to busbar loose or disconnected. Inspect and "
                            + "securely tighten the power wire connecting the contactor output terminals to "
                            + "the busbar.", article(articleRepository, "Fault L32: Wiring from contactor to busbar loose or disconnected"));
            DiagnosticNode lH33 = resolutionNode("cat-h-l33",
                    "Fault L33 \u2014 Sub-circuit distribution wiring or MCB line fault. Check distribution "
                            + "wiring from busbar terminals to individual MCB inputs.", article(articleRepository, "Fault L33: Sub-circuit distribution wiring or MCB line fault"));
            DiagnosticNode lH34 = resolutionNode("cat-h-l34",
                    "Fault L34 \u2014 Incorrect drive control parameter setting. Navigate to parameter Pn000 "
                            + "on the drive keypad and set its value to 3. Alternatively, perform a factory "
                            + "reset on the drive and re-enter drive parameters following the SOP.", article(articleRepository, "Fault L34: Incorrect drive control parameter setting"));
            DiagnosticNode lH35 = resolutionNode("cat-h-l35",
                    "Fault L35 \u2014 Drive run command interlock or mechanical binding. Check PLC run signal "
                            + "output to drive terminals, verify interlock safety circuits, and check the "
                            + "auger shaft for physical obstructions or mechanical jams.", article(articleRepository, "Fault L35: Drive run command interlock or mechanical binding"));

            DiagnosticNode qH26 = questionNode("cat-h-q26",
                    "What is the current value of drive parameter Pn000? (Check via drive keypad display)",
                    false, List.of(
                            treeOption("Parameter Pn000 is NOT set to 3", lH34.getId(), 0),
                            treeOption("Parameter Pn000 is set to 3", lH35.getId(), 1)));
            DiagnosticNode qH25 = questionNode("cat-h-q25",
                    "Is there voltage present on the distribution busbar?", false, List.of(
                            treeOption("No voltage on busbar", lH32.getId(), 0),
                            treeOption("Voltage present on busbar", lH33.getId(), 1)));
            DiagnosticNode qH24 = questionNode("cat-h-q24",
                    "After turning on individual MCBs (SMPS, Load Cell Controller, HMI, PLC, Drive, Energy "
                            + "Meter), do components power up?", false, List.of(
                            treeOption("No, nothing powers up", qH25.getId(), 0),
                            treeOption("Yes, system powers up normally, BUT the Auger motor is not moving",
                                    qH26.getId(), 1)));
            DiagnosticNode qH23 = questionNode("cat-h-q23",
                    "After turning on the Main MCB in the panel, does the main contactor hold in?", false,
                    List.of(
                            treeOption("No, contactor does not hold", lH31.getId(), 0),
                            treeOption("Yes, contactor holds properly", qH24.getId(), 1)));
            DiagnosticNode qH22 = questionNode("cat-h-q22",
                    "What is the status displayed on the VPR after turning on the side rotary switch?", false,
                    List.of(
                            treeOption("VPR displays nothing OR Voltage/Frequency is outside (200-220V, 50 Hz)",
                                    lH30.getId(), 0),
                            treeOption("VPR displays normal (Voltage 200-220V, 50 Hz)", qH23.getId(), 1)));

            // ── Root: category chooser ───────────────────────────────────────
            DiagnosticNode root = questionNode("support-root",
                    "Which system are you having trouble with?", true, List.of(
                            treeOption("Device is not powering on", qA2.getId(), 0),
                            treeOption("Modbus / HMI communication issue", qB6.getId(), 1),
                            treeOption("Azure / Wi-Fi / Telemetry issue", qC8.getId(), 2),
                            treeOption("SD card / data logging issue", qD10.getId(), 3),
                            treeOption("PWM / Blower control issue", qE12.getId(), 4),
                            treeOption("GPS data not showing on the dashboard", qF14.getId(), 5),
                            treeOption("Drive error code \"Er C90\" (encoder feedback)", qG18.getId(), 6),
                            treeOption("System boot failure / auger inactivity", qH22.getId(), 7)));

            nodeRepository.saveAll(List.of(
                    root,
                    qA2, qA3, qA4, qA5, lA1, lA2, lA3, lA4, lA5,
                    qB6, qB7, lB6, lB7, lB8, lB9,
                    qC8, qC9, lC10, lC11, lC12, lC13,
                    qD10, qD11, lD14, lD15, lD16,
                    qE12, qE13, lE17, lE18, lE19,
                    qF14, qF15, qF16, qF17, lF20, lF21, lF22, lF23, lF24,
                    qG18, qG19, qG20, qG21, lG25, lG26, lG27, lG28, lG29,
                    qH22, qH23, qH24, qH25, qH26, lH30, lH31, lH32, lH33, lH34, lH35));
            System.out.println("Seeded the Takavator diagnostic tree (Categories A\u2013H).");
        };
    }

    private static KnowledgeArticle article(KnowledgeArticleRepository articleRepository, String title) {
        return articleRepository.findByTitleIgnoreCase(title).orElse(null);
    }

    private static DiagnosticNode questionNode(String key, String text, boolean root,
                                               List<DiagnosticOption> options) {
        return DiagnosticNode.builder()
                .id(treeId(key)).type("question").text(text).root(root).options(options).build();
    }

    private static DiagnosticNode resolutionNode(String key, String text, KnowledgeArticle article) {
        return DiagnosticNode.builder()
                .id(treeId(key)).type("resolution").text(text).root(false)
                .knowledgeArticleId(article == null ? null : article.getId()).options(List.of()).build();
    }

    private static DiagnosticOption treeOption(String label, UUID destination, int order) {
        return DiagnosticOption.builder().id(UUID.randomUUID()).label(label)
                .destinationNodeId(destination).sortOrder(order).build();
    }

    private static UUID treeId(String key) {
        return UUID.nameUUIDFromBytes(("takachar-diagnostic:" + key)
                .getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private static final String[][] PLACEHOLDER_ARTICLES = {

            // Power Issues
            {
                "Fault L1: Missing or blown fuse",
                "Power Issues",
                "Insert 200mA fuses into both fuse holders (FH1 and FH2) on the PCB. If fuses were already present, they may have blown due to a short circuit or overcurrent. Replace them and inspect the board for any conductive material before powering on again."
            },
            {
                "Fault L2: 12V supply not connected",
                "Power Issues",
                "Connect a stable 12V DC power supply to the 12V power connector on the PCB. Ensure the polarity is correct and the supply provides sufficient current for the board."
            },
            {
                "Fault L3: Power circuit failure, likely buck converter or LDO issue",
                "Power Issues",
                "The 12V to 4.7-4.8V power conversion stage may have failed. Check the TPS54202 buck converter output using a multimeter at the test points. If the output is not within 4.7-4.8V, the power circuit requires inspection or PCB repair. Do not keep operating the board in this state."
            },
            {
                "Fault L4: LDO not producing 3.3V, ESP32-S3 not receiving power",
                "Power Issues",
                "The regulator stepping 4.7-4.8V down to 3.3V for the ESP32-S3 may have failed. Measure the LDO output with a multimeter. If the output is not 3.3V, replace the LDO or open a support ticket with your board serial number."
            },
            {
                "Fault L5: Board is powered but firmware may not be running",
                "Power Issues",
                "Power is reaching the ESP32-S3 correctly, so the issue may be with the firmware. Check if LED D11 (Heartbeat Indicator) is blinking. If not, the firmware may have crashed or is not flashed. Press S2 (Enable Switch) to reset the microprocessor. If the issue persists, reflash the firmware through the USB port."
            },

            // Modbus / HMI
            {
                "Fault L6: RS-485 bus not connected",
                "Modbus / HMI",
                "Securely connect the RS-485 cable to the bus connector on the PCB. Make sure the correct terminals (A, B, GND) are connected according to the wiring diagram. A loose or reversed connection will stop Modbus communication completely."
            },
            {
                "Fault L7: Modbus communication not initiating, possible firmware or transceiver fault",
                "Modbus / HMI",
                "The RS-485 transceiver (MAX22025FAWA+) may be damaged, or the firmware Modbus task is not running. Confirm that D11 (Heartbeat LED) is blinking. If the heartbeat is fine but D4 and D5 stay off, the transceiver IC likely needs replacement. Open a support ticket with your board serial number and setup details."
            },
            {
                "Fault L8: HMI not responding to Modbus requests",
                "Modbus / HMI",
                "The ESP32-S3 is sending Modbus requests (Tx active) but receiving no response from the HMI (Rx silent). Check HMI Modbus settings: baud rate, device address, and parity must match the controller setup. Also verify RS-485 wiring polarity, as swapped A and B lines often cause one-way communication."
            },
            {
                "Fault L9: Modbus communication active but data mismatch",
                "Modbus / HMI",
                "Communication is established, but data values are incorrect. This usually happens when register addresses do not match between the ESP32-S3 firmware and the HMI configuration. Verify that the Modbus register map in the firmware matches the HMI register layout exactly. Cross-check with the provided HMI documentation."
            },

            // Azure / Wi-Fi / Telemetry
            {
                "Fault L10: Wi-Fi antenna not connected",
                "Azure / Wi-Fi / Telemetry",
                "Connect the Wi-Fi antenna to the ESP antenna port on the board. Without the antenna, signal strength is extremely poor and the board will likely fail to connect. Wait 30 to 60 seconds after connecting for the device to reconnect."
            },
            {
                "Fault L11: Wi-Fi not connected, possible credential or signal issue",
                "Azure / Wi-Fi / Telemetry",
                "The board cannot connect to Wi-Fi. Check if Wi-Fi credentials in the firmware are incorrect and reprovision through the SoftAP web portal. Ensure the router is within range and the signal is strong enough. Update credentials if the router SSID or password changed."
            },
            {
                "Fault L12: Azure telemetry connection dropped",
                "Azure / Wi-Fi / Telemetry",
                "The device was connected to Azure IoT Hub but dropped off. This is commonly caused by weak Wi-Fi signal (RSSI worse than -75 dBm), a TCP half-open state, or an expired IoT Hub SAS token. The firmware will attempt automatic reconnection. If it does not reconnect after 5 minutes, press S2 (Enable) to restart the firmware."
            },
            {
                "Fault L13: Telemetry sending but not appearing on Azure IoT Hub",
                "Azure / Wi-Fi / Telemetry",
                "The device shows as connected, but data does not show up on the cloud dashboard. Verify that the correct IoT device ID is configured in the firmware against the Azure IoT Hub registry. Check USB serial logs for any JSON payload errors. Contact your Azure administrator to check routing or consumer group setups."
            },

            // SD Card / Data Logging
            {
                "Fault L14: SD card not inserted",
                "SD Card / Data Logging",
                "Insert a compatible SD card (FAT32 formatted, up to 32GB recommended) into the SD card port until it clicks securely. LED D12 (SD Card Write indicator) should start blinking once logging begins."
            },
            {
                "Fault L15: SD card not being written to, possible format or mount failure",
                "SD Card / Data Logging",
                "Reformat the SD card as FAT32 using a PC and reinsert it. If it still fails, try a different card to rule out corruption. Inspect the SD card module on the PCB for physical connection issues."
            },
            {
                "Fault L16: SD card write corruption",
                "SD Card / Data Logging",
                "Files can corrupt if the board loses power mid-write or if the card is failing. Always power off the board before removing the SD card. Use a high-quality Class 10 card. If issues persist, the SD card module on the PCB may need inspection."
            },

            // PWM / Blower Control
            {
                "Fault L17: Isolated 5V supply missing, PWM output will not work",
                "PWM / Blower Control",
                "The PWM output circuit uses an isolated 5V reference to shift levels from the 3.3V logic on the ESP32-S3. Without this 5V supply, the PWM connector produces no signal. Connect the 5V isolated power supply to the designated connector on the left side of the enclosure."
            },
            {
                "Fault L18: PWM output connector not wired to blowers",
                "PWM / Blower Control",
                "Connect the blower control cables to the Output PWM Connector on the PCB (accessible from the left side of the enclosure). Make sure each blower is connected to its assigned channel per the wiring diagram."
            },
            {
                "Fault L19: PWM signal present but blowers not responding",
                "PWM / Blower Control",
                "Verify that PWM duty cycle settings in the firmware match what the blower driver expects. Check the blower driver or VFD for fault codes. Test the PWM output voltage with a multimeter or oscilloscope at the connector to confirm a 5V referenced signal. If the signal is present, the issue is with the blower or driver rather than the PCB."
            },

            // GPS / Location Data
            {
                "Fault L20: Dashboard not receiving any data because Wi-Fi is down",
                "GPS / Location Data",
                "The dashboard cannot receive location data without an active Wi-Fi connection. Resolve Wi-Fi first: check the Wi-Fi antenna connection, confirm credentials via the SoftAP portal, and make sure the router is in range."
            },
            {
                "Fault L21: GPS antenna not connected to the board",
                "GPS / Location Data",
                "Attach the GPS antenna to the GPS port on the PCB. The Neo-M8N GPS module cannot receive satellite signals without an antenna. Make sure the connector is seated firmly."
            },
            {
                "Fault L22: GPS antenna has no clear view of the sky",
                "GPS / Location Data",
                "Place the GPS antenna outdoors or in an unobstructed location to receive satellite signals. Route the cable so the antenna sits open to the sky. Wait 1 to 3 minutes for satellite fix acquisition."
            },
            {
                "Fault L23: GPS module still acquiring satellite fix",
                "GPS / Location Data",
                "The Neo-M8N module needs 1 to 3 minutes to lock onto satellites after power-on, especially during a cold start. Wait at least 3 minutes before expecting data on the dashboard."
            },
            {
                "Fault L24: GPS module not producing data",
                "GPS / Location Data",
                "Confirm 3.3V power is reaching the GPS module. Check the UART communication between the GPS module and ESP32-S3 via the USB serial monitor to look for NMEA sentences. If no NMEA data appears, the GPS module may be damaged and requires a support ticket."
            },

            // Drive Encoder Faults (Er C90)
            {
                "Fault L25: Loose or disconnected encoder cable connectors",
                "Drive Encoder Faults",
                "Check the connectors on both ends (drive side and motor side). Ensure both sides are securely inserted and locked in place properly."
            },
            {
                "Fault L26: Damaged encoder cable",
                "Drive Encoder Faults",
                "Replace the damaged encoder cable (e.g., cuts or heat damage from exposure to hot char in the field). Preventive action: route all cables inside protective cable ducts and inspect them routinely to protect against field char damage."
            },
            {
                "Fault L27: Damaged connector pins on the motor encoder socket",
                "Drive Encoder Faults",
                "Carefully straighten bent pins or replace the damaged encoder connector assembly on the motor."
            },
            {
                "Fault L28: Internal encoder failure on the motor",
                "Drive Encoder Faults",
                "Replace the faulty motor or replace its internal encoder unit."
            },
            {
                "Fault L29: Drive parameter corruption or internal drive controller fault",
                "Drive Encoder Faults",
                "Perform a factory reset on the drive parameters and re-configure all parameter settings according to the drive manual SOP."
            },

            // Boot Failure & Auger Inactivity
            {
                "Fault L30: Damaged VPR or out-of-spec incoming power supply",
                "Boot Failure & Auger Inactivity",
                "Check incoming PDB voltage. If incoming line voltage is within range (200-220V, 50 Hz) but VPR displays nothing or stays tripped, the VPR unit is damaged and requires replacement."
            },
            {
                "Fault L31: Faulty main contactor or control loop fault",
                "Boot Failure & Auger Inactivity",
                "Verify control voltage across the contactor coil. If control voltage is present but the contactor fails to pull in and hold, replace the faulty contactor."
            },
            {
                "Fault L32: Wiring from contactor to busbar loose or disconnected",
                "Boot Failure & Auger Inactivity",
                "Inspect and securely tighten the power wire connecting the contactor output terminals to the busbar."
            },
            {
                "Fault L33: Sub-circuit distribution wiring or MCB line fault",
                "Boot Failure & Auger Inactivity",
                "Check distribution wiring from busbar terminals to individual MCB inputs."
            },
            {
                "Fault L34: Incorrect drive control parameter setting",
                "Boot Failure & Auger Inactivity",
                "Navigate to parameter Pn000 on the drive keypad and set its value to 3. Alternatively, perform a factory reset on the drive and re-enter drive parameters following the SOP."
            },
            {
                "Fault L35: Drive run command interlock or mechanical binding",
                "Boot Failure & Auger Inactivity",
                "Check PLC run signal output to drive terminals, verify interlock safety circuits, and check the auger shaft for physical obstructions or mechanical jams."
            }
    };
}