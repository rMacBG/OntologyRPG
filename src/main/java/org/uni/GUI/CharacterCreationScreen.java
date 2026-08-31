package org.uni.GUI;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.function.BiConsumer;

public class CharacterCreationScreen extends VBox {

    public record HeroClassOption(String id, String labelText, String defaultWeapon) {}

    public CharacterCreationScreen(BiConsumer<HeroClassOption, String> onConfirmSelection) {
        setAlignment(Pos.CENTER);
        setSpacing(20);
        setPadding(new Insets(30));

        Label header = new Label("CHOOSE YOUR CLASS");
        header.setStyle("-fx-text-fill: white; -fx-font-size: 24px; -fx-font-weight: bold;");
        getChildren().add(header);

        ToggleGroup group = new ToggleGroup();

        List<HeroClassOption> classOptions = List.of(
                new HeroClassOption("WarriorClass", "Warrior (150 HP, 15 ATK) - Starts with Iron Longsword", "IronLongSword"),
                new HeroClassOption("ArcherClass", "Archer (120 HP, 17 ATK) - Starts with Small Bow", "SmallBow"),
                new HeroClassOption("WizardClass", "Wizard (100 HP, 22 ATK) - Starts with Storm Staff", "StormStaff"),
                new HeroClassOption("AssassinClass", "Assassin (135 HP, 20 ATK) - Starts with Steel Dagger", "SteelDagger")
        );

        for (int i = 0; i < classOptions.size(); i++) {
            HeroClassOption option = classOptions.get(i);
            RadioButton rb = new RadioButton(option.labelText());
            rb.setToggleGroup(group);
            rb.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");
            rb.setUserData(option);

            if (i == 0) {
                rb.setSelected(true);
            }

            getChildren().add(rb);
        }

        Button confirmBtn = new Button("UNFOLD THE ADVENTURE OF A MILLENIA!");
        confirmBtn.setPrefSize(220, 45);

        confirmBtn.setOnAction(e -> {
            RadioButton selected = (RadioButton) group.getSelectedToggle();
            if (selected != null && onConfirmSelection != null) {
                HeroClassOption selectedOption = (HeroClassOption) selected.getUserData();

                String startingSkill = switch (selectedOption.id()) {
                    case "WarriorClass" -> "SteelHelmet";
                    case "ArcherClass" -> "LeatherQuiver";
                    case "WizardClass" -> "ChargedLightning";
                    case "AssassinClass" -> "FissureGrenade";
                    default -> "SteelHelmet";
                };

                onConfirmSelection.accept(selectedOption, startingSkill);
            }
        });

        getChildren().add(confirmBtn);
    }
}