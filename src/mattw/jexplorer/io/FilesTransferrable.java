package mattw.jexplorer.io;

import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class FilesTransferrable implements Transferable, ClipboardOwner {
	
	private List<File> list;

	public FilesTransferrable(List<File> files) {
		this.list = files;
	}

	@Override
	public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
		if (flavor == DataFlavor.javaFileListFlavor) {
			return list;
		}

		return new UnsupportedFlavorException(flavor);
	}

	@Override
	public DataFlavor[] getTransferDataFlavors() {
		DataFlavor[] dataFlavors = new DataFlavor[1];
		dataFlavors[0] = DataFlavor.javaFileListFlavor;
		return dataFlavors;
	}

	@Override
	public boolean isDataFlavorSupported(DataFlavor flavor) {
		return flavor == DataFlavor.javaFileListFlavor;
	}

	@Override
	public void lostOwnership(java.awt.datatransfer.Clipboard clipboard, Transferable contents) {

	}
}