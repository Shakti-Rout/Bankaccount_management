import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Scanner;

public class bankaccounts {
	private final Map<String, BankAccount> accounts = new LinkedHashMap<>();
	private final Scanner scanner = new Scanner(System.in);

	public static void main(String[] args) {
		new bankaccounts().run();
	}

	private void run() {
		boolean running = true;
		while (running) {
			System.out.println("\n=== Bank Account Management ===");
			System.out.println("1. Open savings account");
			System.out.println("2. Open checking account");
			System.out.println("3. Deposit");
			System.out.println("4. Withdraw");
			System.out.println("5. View account");
			System.out.println("6. List accounts");
			System.out.println("0. Exit");
			System.out.print("Choose an option: ");

			switch (scanner.nextLine().trim()) {
				case "1":
					openAccount(true);
					break;
				case "2":
					openAccount(false);
					break;
				case "3":
					deposit();
					break;
				case "4":
					withdraw();
					break;
				case "5":
					viewAccount();
					break;
				case "6":
					listAccounts();
					break;
				case "0":
					running = false;
					System.out.println("Goodbye.");
					break;
				default:
					System.out.println("Please choose a listed option.");
			}
		}
	}

	private void openAccount(boolean savings) {
		System.out.print("Account number: ");
		String number = scanner.nextLine().trim();
		if (number.isEmpty()) {
			System.out.println("Account number cannot be empty.");
			return;
		}
		if (accounts.containsKey(number)) {
			System.out.println("That account number is already in use.");
			return;
		}

		System.out.print("Account holder: ");
		String holder = scanner.nextLine().trim();
		if (holder.isEmpty()) {
			System.out.println("Account holder cannot be empty.");
			return;
		}
		BigDecimal openingBalance = readAmount("Opening balance: ");
		if (openingBalance == null || openingBalance.signum() < 0) {
			System.out.println("Opening balance must be zero or greater.");
			return;
		}

		BankAccount account = savings
				? new SavingsAccount(number, holder, openingBalance)
				: new CheckingAccount(number, holder, openingBalance);
		accounts.put(number, account);
		System.out.println("Opened " + account.getAccountType() + " account.");
	}

	private void deposit() {
		BankAccount account = findAccount();
		if (account == null) {
			return;
		}
		BigDecimal amount = readAmount("Deposit amount: ");
		if (amount == null) {
			return;
		}
		try {
			account.deposit(amount);
			System.out.println("Deposit complete. Balance: " + formatMoney(account.getBalance()));
		} catch (IllegalArgumentException exception) {
			System.out.println(exception.getMessage());
		}
	}

	private void withdraw() {
		BankAccount account = findAccount();
		if (account == null) {
			return;
		}
		BigDecimal amount = readAmount("Withdrawal amount: ");
		if (amount == null) {
			return;
		}
		try {
			account.withdraw(amount);
			System.out.println("Withdrawal complete. Balance: " + formatMoney(account.getBalance()));
		} catch (InsufficientBalanceException | IllegalArgumentException exception) {
			System.out.println(exception.getMessage());
		}
	}

	private void viewAccount() {
		BankAccount account = findAccount();
		if (account != null) {
			System.out.println(account);
		}
	}

	private void listAccounts() {
		if (accounts.isEmpty()) {
			System.out.println("No accounts have been opened.");
			return;
		}
		for (BankAccount account : accounts.values()) {
			System.out.println(account);
		}
	}

	private BankAccount findAccount() {
		System.out.print("Account number: ");
		BankAccount account = accounts.get(scanner.nextLine().trim());
		if (account == null) {
			System.out.println("Account not found.");
		}
		return account;
	}

	private BigDecimal readAmount(String prompt) {
		System.out.print(prompt);
		try {
			return new BigDecimal(scanner.nextLine().trim());
		} catch (NumberFormatException exception) {
			System.out.println("Enter a valid amount.");
			return null;
		}
	}

	private static String formatMoney(BigDecimal amount) {
		return "$" + amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
	}
}

abstract class BankAccount {
	private final String accountNumber;
	private final String accountHolder;
	private BigDecimal balance;

	protected BankAccount(String accountNumber, String accountHolder, BigDecimal openingBalance) {
		if (accountNumber == null || accountNumber.isBlank()) {
			throw new IllegalArgumentException("Account number cannot be empty.");
		}
		if (accountHolder == null || accountHolder.isBlank()) {
			throw new IllegalArgumentException("Account holder cannot be empty.");
		}
		if (openingBalance.signum() < 0) {
			throw new IllegalArgumentException("Opening balance cannot be negative.");
		}
		this.accountNumber = accountNumber;
		this.accountHolder = accountHolder;
		this.balance = openingBalance;
	}

	public final void deposit(BigDecimal amount) {
		requirePositive(amount);
		balance = balance.add(amount);
	}

	public abstract void withdraw(BigDecimal amount) throws InsufficientBalanceException;

	public abstract String getAccountType();

	protected final void requirePositive(BigDecimal amount) {
		if (amount == null || amount.signum() <= 0) {
			throw new IllegalArgumentException("Amount must be greater than zero.");
		}
	}

	public final BigDecimal getBalance() {
		return balance;
	}

	protected final void setBalance(BigDecimal balance) {
		this.balance = balance;
	}

	@Override
	public String toString() {
		return getAccountType() + " | Account: " + accountNumber
				+ " | Holder: " + accountHolder
				+ " | Balance: $" + balance.setScale(2, RoundingMode.HALF_UP).toPlainString();
	}
}

class SavingsAccount extends BankAccount {
	private static final BigDecimal MINIMUM_BALANCE = new BigDecimal("100.00");

	SavingsAccount(String accountNumber, String accountHolder, BigDecimal openingBalance) {
		super(accountNumber, accountHolder, openingBalance);
	}

	@Override
	public void withdraw(BigDecimal amount) throws InsufficientBalanceException {
		requirePositive(amount);
		BigDecimal remaining = getBalance().subtract(amount);
		if (remaining.compareTo(MINIMUM_BALANCE) < 0) {
			throw new InsufficientBalanceException(
					"Savings accounts must retain at least $" + MINIMUM_BALANCE.toPlainString() + ".");
		}
		setBalance(remaining);
	}

	@Override
	public String getAccountType() {
		return "Savings";
	}
}

class CheckingAccount extends BankAccount {
	private static final BigDecimal OVERDRAFT_LIMIT = new BigDecimal("500.00");

	CheckingAccount(String accountNumber, String accountHolder, BigDecimal openingBalance) {
		super(accountNumber, accountHolder, openingBalance);
	}

	@Override
	public void withdraw(BigDecimal amount) throws InsufficientBalanceException {
		requirePositive(amount);
		BigDecimal remaining = getBalance().subtract(amount);
		if (remaining.compareTo(OVERDRAFT_LIMIT.negate()) < 0) {
			throw new InsufficientBalanceException(
					"Withdrawal exceeds the $" + OVERDRAFT_LIMIT.toPlainString() + " overdraft limit.");
		}
		setBalance(remaining);
	}

	@Override
	public String getAccountType() {
		return "Checking";
	}
}

class InsufficientBalanceException extends Exception {
	InsufficientBalanceException(String message) {
		super(message);
	}
}
