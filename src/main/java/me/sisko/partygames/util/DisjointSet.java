package me.sisko.partygames.util;
import java.util.ArrayList;

public class DisjointSet {
    private ArrayList<Integer> dset;
    public DisjointSet() {
        dset = new ArrayList<Integer>();
    }
    void addelements(int num) {
        for(int i = 0; i < num; i++) {
            dset.add(-1);
        }
    }
    final int find(int i) {
        if(dset.get(i) < 0) {
            return i;
        }
        dset.set(i, find(dset.get(i)));
        return dset.get(i);
    }

    void union(int a, int b) {
        int roota = find(a);
        int rootb = find(b);
        int addSize = dset.get(roota) + dset.get(rootb);
        if(roota == rootb) {
            return;
        }
        if(dset.get(roota) > dset.get(rootb)) {
            dset.set(roota, addSize);
            dset.set(rootb, roota);
        }
        else {
            dset.set(roota, rootb);
            dset.set(rootb, addSize);
        }
    }
    final int size(int elem) {
        return -1 * find(elem);
    }

}
