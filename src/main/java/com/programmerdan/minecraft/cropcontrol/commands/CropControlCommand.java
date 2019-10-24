package com.programmerdan.minecraft.cropcontrol.commands;

import java.util.List;

import org.bukkit.command.CommandSender;

import com.programmerdan.minecraft.cropcontrol.config.DropConfig;
import com.programmerdan.minecraft.cropcontrol.config.RootConfig;
import com.programmerdan.minecraft.cropcontrol.config.ToolConfig;

import vg.civcraft.mc.civmodcore.command.CivCommand;
import vg.civcraft.mc.civmodcore.command.StandaloneCommand;

@CivCommand(id = "cropcontrol")
public class CropControlCommand extends StandaloneCommand{

	@Override
	public boolean execute(CommandSender sender, String[] args) {
		RootConfig.reload();
		DropConfig.reload();
		ToolConfig.clear();
		sender.sendMessage("Configuration cleared, will progressively reload.");
		return true;
	}

	@Override
	public List<String> tabComplete(CommandSender sender, String[] args) {
		return null;
	}

}
