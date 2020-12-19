/*
 * Decompiled with CFR 0.150.
 */
package org.zeith.darktheme.internal.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class ReleasableFileInputStream
extends FileInputStream {
    Runnable released;

    public ReleasableFileInputStream(File file, Runnable released) throws FileNotFoundException {
        super(file);
        this.released = released;
    }

    @Override
    public void close() throws IOException {
        super.close();
        this.released.run();
    }
}

