package com.github.beastyboo.advancedjail.adapter.cached;

import com.github.beastyboo.advancedjail.application.AJail;
import com.github.beastyboo.advancedjail.config.JailConfiguration;
import com.github.beastyboo.advancedjail.domain.MessageType;
import com.github.beastyboo.advancedjail.domain.entity.Cell;
import com.github.beastyboo.advancedjail.domain.entity.Inmate;
import com.github.beastyboo.advancedjail.domain.entity.Jail;
import com.github.beastyboo.advancedjail.domain.port.CellRepository;
import com.github.beastyboo.advancedjail.util.BasicUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Created by Torbie on 10.12.2020.
 */
public class CellMemory implements CellRepository{

    private final AJail core;
    private final Map<UUID, Cell> cells;
    private final JailConfiguration api;

    public CellMemory(AJail core) {
        this.core = core;
        cells = new HashMap<>();
        api = core.getAPI();
    }

    @Override
    public void load() {

    }

    @Override
    public void close() {

    }

    @Override
    public boolean createCell(String jailName, String name, Player player, int size) {
        Optional<Jail> jail = api.getJailByName(jailName);
        if(!jail.isPresent()) {
            core.message(player, MessageType.JAIL_NOT_FOUND);
            return false;
        }

        if(jail.get().getCells().get(name.toLowerCase()) != null) {
            core.message(player, MessageType.CELL_NAME_TAKEN);
            return false;
        }

        if(size <= 0) {
            core.message(player, MessageType.CELL_SIZE_INVALID);
            return false;
        }

        if(BasicUtil.isPlayerInJail(player.getWorld(), jail.get().getName(), player.getLocation()) == false) {
            core.message(player, MessageType.PLAYER_OUTSIDE_CELL);
            return false;
        }

        Map<UUID, Inmate> players = new HashMap<>();
        Cell cell = new Cell(UUID.randomUUID(), name, player.getLocation(), size, players);
        jail.get().getCells().put(cell.getName().toLowerCase(), cell);
        cells.put(cell.getId(), cell);
        core.message(player, MessageType.CELL_CREATED);
        return true;
    }

    @Override
    public boolean deleteCell(String jailName, String name, Player player) {
        Optional<Jail> jail = api.getJailByName(jailName);
        if(!jail.isPresent()) {
            core.message(player, MessageType.JAIL_NOT_FOUND);
            return false;
        }

        Cell cell = jail.get().getCells().get(name.toLowerCase());
        if(cell == null) {
            core.message(player, MessageType.CELL_NOT_FOUND);
            return false;
        }

        for(Inmate inmate : cell.getPlayers().values()) {
            Player pInmate = Bukkit.getPlayer(inmate.getUuid());
            core.getAPI().releasePlayer(Optional.empty(), pInmate, false);
        }

        //TODO:
        //  - delete file.

        jail.get().getCells().remove(cell.getName().toLowerCase(), cell);
        cells.remove(cell.getId(), cell);
        core.message(player, MessageType.CELL_DELETED);
        return true;
    }

    @Override
    public Optional<Cell> getCellByUUID(UUID uuid) {
        return Optional.ofNullable(cells.get(uuid));
    }

    @Override
    public Optional<Cell> getCellByJailAndName(String jailName, String name) {
        Optional<Jail> jail = api.getJailByName(jailName);
        if(!jail.isPresent()) {
            return Optional.empty();
        }

        return Optional.ofNullable(jail.get().getCells().get(name.toLowerCase()));
    }

    @Override
    public Optional<Cell> getCellByInmate(UUID uuid) {
        for(Cell cell : cells.values()) {
            if(cell.getPlayers().get(uuid) != null) {
                return Optional.ofNullable(cell);
            }
        }
        return Optional.empty();
    }

    @Override
    public Set<Cell> getAllCells() {
        return new HashSet<>(cells.values());
    }
}
