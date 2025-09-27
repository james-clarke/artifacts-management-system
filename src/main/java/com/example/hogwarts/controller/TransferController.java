package com.example.hogwarts.controller;

import com.example.hogwarts.data.DataStore;
import com.example.hogwarts.model.Artifact;
import com.example.hogwarts.model.Wizard;
import com.example.hogwarts.model.Transfer;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class TransferController {
    private final DataStore store = DataStore.getInstance();

    public Collection<Transfer> findAllTransfersById(int id) {
    	return this.store.findAllTransfersById(id);
    }

    public Transfer addTransfer(String type, Artifact artifact, Wizard wizard) {
        Transfer transfer = new Transfer(type, artifact, wizard);
        return this.store.addTransfer(transfer);
    }
}
